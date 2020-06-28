package info.ginj.export.online.google;

import com.google.gson.Gson;
import info.ginj.export.online.AbstractOAuth2Exporter;
import info.ginj.export.online.exception.AuthorizationException;
import info.ginj.export.online.exception.CommunicationException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;

import javax.swing.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * Handles interaction with a Google service
 *
 * OAuth2 authorization flow based on
 * https://developers.google.com/identity/protocols/oauth2
 * and
 * https://developers.google.com/identity/protocols/oauth2/native-app#obtainingaccesstokens
 * <p>
 * TODO: only keep a single HttpClient ?
 */
public abstract class GoogleExporter extends AbstractOAuth2Exporter {
    private static final String GOOGLE_CLIENT_APP_KEY = "805469689820-c3drai5blocq5ae120md067te73ejv49.apps.googleusercontent.com";
    private static final String GOOGLE_NOT_SO_SECRET_CLIENT_APP_KEY = "2guKmYBdrb1nhGkMgdSrbeXl"; // "In this context, the client secret is obviously not treated as a secret." ( https://developers.google.com/identity/protocols/oauth2 )
    private static final String GOOGLE_OAUTH2_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_OAUTH2_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_OAUTH2_REVOKE_URL = "https://myaccount.google.com/permissions";

    public GoogleExporter(JFrame frame) {
        super(frame);
    }

    @Override
    protected String getClientAppId() {
        return GOOGLE_CLIENT_APP_KEY;
    }

    @Override
    protected String getSecretAppKey() {
        return GOOGLE_NOT_SO_SECRET_CLIENT_APP_KEY;
    }

    @Override
    protected String getOAuth2AuthUrl() {
        return GOOGLE_OAUTH2_AUTH_URL;
    }

    @Override
    protected String getOAuth2TokenUrl() {
        return GOOGLE_OAUTH2_TOKEN_URL;
    }

    @Override
    protected String getOAuth2RevokeUrl() {
        return GOOGLE_OAUTH2_REVOKE_URL;
    }

    public void checkAuthorizations(String accountNumber) throws CommunicationException, AuthorizationException {
        logProgress("Checking authorizations", 2);
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet;
        try {
            URIBuilder builder = new URIBuilder("https://www.googleapis.com/oauth2/v3/tokeninfo");
            builder.setParameter("access_token", getAccessToken(accountNumber));
            httpGet = new HttpGet(builder.build());
        }
        catch (URISyntaxException e) {
            throw new CommunicationException(e);
        }
        try {
            CloseableHttpResponse response = client.execute(httpGet);
            if (isStatusOK(response.getCode())) {
                final String responseText;
                try {
                    responseText = EntityUtils.toString(response.getEntity());
                }
                catch (ParseException e) {
                    throw new CommunicationException("Could not parse album list response as String: " + response.getEntity());
                }
                @SuppressWarnings("rawtypes")
                Map map = new Gson().fromJson(responseText, Map.class);
                String error = (String) map.get("error");
                String errorDescription = (String) map.get("errorDescription");
                String scopeStr = (String) map.get("scope");

                if ((error != null && !error.isBlank()) || (errorDescription != null && !errorDescription.isBlank())) {
                    String msg = "";
                    if (error != null) {
                        msg = "Error: " + error;
                    }
                    if (errorDescription != null) {
                        msg += " (" + errorDescription + ")";
                    }
                    // Remove stored tokens to force a reauthorization
                    clearOAuthTokens(accountNumber);
                    throw new AuthorizationException(msg + ". Please re-authorize...");
                }

                if (scopeStr == null || scopeStr.isBlank()) {
                    // Remove stored tokens to force a reauthorization
                    clearOAuthTokens(accountNumber);
                    throw new AuthorizationException("No scope is defined for this token. Please re-authorize...");
                }

                final List<String> missingScopes = getMissingScopes(scopeStr);
                if (!missingScopes.isEmpty()) {
                    throw new AuthorizationException("The authorizations below are missing for " + getExporterName() + ". Please re-authorize this account.\n Missing scopes: " + missingScopes);
                }
            }
            else {
                throw new AuthorizationException("Server returned an error when listing albums: " + getResponseError(response));
            }
        }
        catch (IOException e) {
            throw new CommunicationException(e);
        }
    }

}
