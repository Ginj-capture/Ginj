package info.ginj.export.online.google;

import com.google.gson.Gson;
import info.ginj.export.online.AbstractOAuth2Exporter;
import info.ginj.export.online.OAuthAccount;
import info.ginj.export.online.exception.AuthorizationException;
import info.ginj.export.online.exception.CommunicationException;
import info.ginj.model.Account;
import info.ginj.model.Profile;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
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

    // Access to display username and email
    private static final String[] GOOGLE_PROFILE_REQUIRED_SCOPES = {"https://www.googleapis.com/auth/userinfo.profile", "https://www.googleapis.com/auth/userinfo.email"};


    @Override
    protected String getClientAppId() {
        return GOOGLE_CLIENT_APP_KEY;
    }

    @Override
    protected String getSecretAppKey() {
        return GOOGLE_NOT_SO_SECRET_CLIENT_APP_KEY;
    }

    @Override
    protected String getOAuth2AuthorizeUrl() {
        return GOOGLE_OAUTH2_AUTH_URL;
    }

    @Override
    protected String getOAuth2TokenUrl() {
        return GOOGLE_OAUTH2_TOKEN_URL;
    }

    @Override
    public String getOAuth2RevokeUrl() {
        return GOOGLE_OAUTH2_REVOKE_URL;
    }

    @Override
    protected List<String> getRequiredScopes() {
        return Arrays.asList(GOOGLE_PROFILE_REQUIRED_SCOPES);
    }

    @Override
    public boolean isOnlineService() {
        return true;
    }

    /**
     * This method checks that requested Google authorizations (scopes) are still OK.
     *
     * @param account the account to validate
     * @throws CommunicationException in case a communication error occurs
     * @throws AuthorizationException in case authorization fails
     */
    public void checkAuthorizations(Account account) throws CommunicationException, AuthorizationException {
        logProgress("Checking authorizations", 2);
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet;
        try {
            URIBuilder builder = new URIBuilder("https://www.googleapis.com/oauth2/v3/tokeninfo");
            builder.setParameter("access_token", getAccessToken(account));
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
                    throw new CommunicationException("Could not parse token info response as String: " + response.getEntity());
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
                    clearOAuthTokens((OAuthAccount) account);
                    throw new AuthorizationException(msg + ". Please re-authorize...");
                }

                if (scopeStr == null || scopeStr.isBlank()) {
                    // Remove stored tokens to force a reauthorization
                    clearOAuthTokens((OAuthAccount) account);
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


    @Override
    protected Profile getProfile(String accessToken) throws CommunicationException, AuthorizationException {
        CloseableHttpClient client = HttpClients.createDefault();

        HttpGet httpGet;
        try {
            URIBuilder builder = new URIBuilder("https://www.googleapis.com/oauth2/v1/userinfo?alt=json");
            httpGet = new HttpGet(builder.build());
        }
        catch (URISyntaxException e) {
            throw new CommunicationException(e);
        }

        httpGet.addHeader("Authorization", "Bearer " + accessToken);

        try {
            CloseableHttpResponse response = client.execute(httpGet);
            if (isStatusOK(response.getCode())) {
                final String responseText;
                try {
                    responseText = EntityUtils.toString(response.getEntity());
                    // Sample response:
                    // {
                    //  "id": "123456789012345678901",
                    //  "email": "username@gmail.com",
                    //  "verified_email": true,
                    //  "name": "My Name",
                    //  "given_name": "My",
                    //  "family_name": "Name",
                    //  "picture": "https://lh4.googleusercontent.com/..../photo.jpg",
                    //  "locale": "en"
                    //}
                    @SuppressWarnings("rawtypes")
                    Map map = new Gson().fromJson(responseText, Map.class);
                    Profile profile = new Profile();
                    profile.setEmail((String) map.get("email"));
                    profile.setName((String) map.get("name"));
                    return profile;
                }
                catch (ParseException e) {
                    throw new CommunicationException("Could not parse account information response as String: " + response.getEntity());
                }
            }
            else {
                throw new CommunicationException("Server returned an error when listing albums: " + getResponseError(response));
            }
        }
        catch (IOException e) {
            throw new CommunicationException(e);
        }
    }
}
