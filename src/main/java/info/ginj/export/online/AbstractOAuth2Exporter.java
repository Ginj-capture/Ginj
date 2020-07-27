package info.ginj.export.online;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import info.ginj.Ginj;
import info.ginj.export.Exporter;
import info.ginj.export.online.exception.AuthorizationException;
import info.ginj.export.online.exception.CommunicationException;
import info.ginj.model.Account;
import info.ginj.model.Capture;
import info.ginj.model.Profile;
import info.ginj.model.Target;
import info.ginj.util.UI;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Handles OAuth 2 authorization protocol, used e.g. by Google and Dropbox
 * see https://developers.google.com/identity/protocols/oauth2
 * see https://developers.google.com/identity/protocols/oauth2/native-app
 * see https://www.dropbox.com/lp/developers/reference/oauth-guide
 */
public abstract class AbstractOAuth2Exporter extends Exporter implements OnlineExporter {
    public static final String HTML_BODY_OPEN = "<html><head><style>body{background-color:" + UI.colorToHex(UI.LABEL_BACKGROUND_COLOR) + ";font-family:sans-serif;color:" + UI.colorToHex(UI.LABEL_FOREGROUND_COLOR) + ";} a{color:" + UI.colorToHex(UI.ICON_ENABLED_COLOR) + ";} a:hover{color:white;}</style></head><body>";
    public static final String BODY_HTML_CLOSE = "</body></html>";
    protected static final int PORT_GINJ = 6193;

    // TODO mabye make the following fields ThreadLocal ?
    protected String verifier;
    protected String receivedCode = null;
    protected ArrayList<String> receivedScopes = null;
    protected HttpServer server;


    @java.beans.Transient
    protected abstract String getClientAppId();

    @java.beans.Transient
    protected abstract String getSecretAppKey();

    @java.beans.Transient
    protected abstract String getOAuth2AuthorizeUrl();

    @java.beans.Transient
    protected String getAdditionalAuthorizeParam() {
        return "";
    }

    @java.beans.Transient
    public abstract String getOAuth2RevokeUrl();

    @java.beans.Transient
    protected abstract List<String> getRequiredScopes();

    @Override
    public boolean prepare(Capture capture, Target target) {
        try {
            checkAuthorizations(target.getAccount());
        }
        catch (AuthorizationException e) {
            UI.alertException(parentFrame, getExporterName() + " authorization error", "Ginj was not authorized on " + getExporterName() + ".\nPlease go to " + Ginj.getAppName() + " preferences to re-authorize.", e);
            failed("Authorization error");
            return false;
        }
        catch (CommunicationException e) {
            UI.alertException(parentFrame, getExporterName() + " authorization check error", "There was an error checking authorization on " + getExporterName(), e);
            failed("Communication error");
            return false;
        }
        return true;
    }

    public OAuthAccount authorize() throws AuthorizationException, CommunicationException {
        OAuthAccount oAuthAccount = null;
        try {
            // Start web server to receive Google responses
            server = getHttpServer();
            server.start();

            // Step 1: Generate a code verifier and challenge

            // Create a Code Verifier
            SecureRandom sr = new SecureRandom();
            byte[] code = new byte[32];
            sr.nextBytes(code);
            final Base64.Encoder encoder = Base64.getUrlEncoder();
            verifier = encoder.encodeToString(code).replaceAll("=+$", ""); // replacement required for Dropbox
            // System.out.println("verifier = " + verifier);

            // Create a Code Challenge
            byte[] bytes = verifier.getBytes(StandardCharsets.US_ASCII);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(bytes, 0, bytes.length);
            byte[] digest = md.digest();
            final String challenge = encoder.encodeToString(digest).replaceAll("=+$", ""); // replacement required for Google
            // System.out.println("challenge = " + challenge);

            // Prepare the URL to forward the user to
            String url = getOAuth2AuthorizeUrl()
                    + "?client_id=" + getClientAppId()
                    + "&response_type=code"
                    + "&code_challenge=" + challenge
                    + "&code_challenge_method=S256"
                    + getAdditionalAuthorizeParam()
                    // for local server, use:
                    + "&redirect_uri=" + URLEncoder.encode("http://localhost:" + PORT_GINJ, UTF_8);
            // for Google copy/paste mode, use:
            // + "&redirect_uri=" + URLEncoder.encode("urn:ietf:wg:oauth:2.0:oob", UTF_8);

            final List<String> requiredScopes = getRequiredScopes();
            if (requiredScopes != null) {
                url += "&scope=" + encodeScopes(requiredScopes);
            }
            // System.out.println("url = " + url);

            // Step 2: Send a request to the OAuth 2.0 server

            // Open that page in the user's default browser
            logProgress("Waiting for browser authorization");
            Desktop.getDesktop().browse(new URI(url));

            // (Step 3: Online Service prompts user for consent)

            // Step 4: Handle the OAuth 2.0 server response (see async http server code)

            // Wait for code to be received by our http server...
            long timeOutTime = System.currentTimeMillis() + 5 * 60 * 1000;
            // TODO make sure the progress indicates that browser is opening, and that operation can be cancelled.
            while (receivedCode == null && System.currentTimeMillis() < timeOutTime && !cancelRequested()) {
                //noinspection BusyWait
                Thread.sleep(100);
            }
            // When we get here, it's because of either an abort request, a response, or a time-out
            if (!cancelRequested()) {
                if (receivedCode != null) {
                    // Step 5: Exchange authorization code for refresh and access tokens
                    oAuthAccount = exchangeCodeForTokens(receivedCode, receivedScopes);
                }
                else {
                    // time-out
                    throw new AuthorizationException("Time out waiting for authorization");
                }
            }
        }
        catch (NoSuchAlgorithmException | URISyntaxException | IOException | InterruptedException e) {
            throw new CommunicationException(e);
        }
        finally {
            if (server != null) {
                // Shutdown server
                server.stop(2);
            }
        }
        return oAuthAccount;
    }

    @java.beans.Transient
    protected abstract String getOAuth2TokenUrl();

    // Note: forcing an ArrayList because it will have to be persisted and we have to make sure it has a no-arg constructor (e.g. not Arrays.asList())
    protected OAuthAccount exchangeCodeForTokens(String code, ArrayList<String> allowedScopes) throws AuthorizationException, CommunicationException {
        logProgress("Getting tokens");

        CloseableHttpClient client = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost(getOAuth2TokenUrl());

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("client_id", getClientAppId()));
        params.add(new BasicNameValuePair("code", code));
        params.add(new BasicNameValuePair("code_verifier", verifier));
        params.add(new BasicNameValuePair("grant_type", "authorization_code"));
        params.add(new BasicNameValuePair("redirect_uri", "http://localhost:" + PORT_GINJ)); // No idea why that URL must be resent
        if (getSecretAppKey() != null) {
            params.add(new BasicNameValuePair("client_secret", getSecretAppKey()));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(params, UTF_8));

        try {
            CloseableHttpResponse response = client.execute(httpPost);

            if (isStatusOK(response.getCode())) {
                final String responseText;
                try {
                    responseText = EntityUtils.toString(response.getEntity());
                }
                catch (ParseException e) {
                    throw new AuthorizationException("Could not parse server response as String: " + response.getEntity());
                }
                @SuppressWarnings("rawtypes")
                Map map = new Gson().fromJson(responseText, Map.class);
                String accessToken = (String) map.get("access_token");
                Double expiresInSecs = (Double) map.get("expires_in");
                String refreshToken = (String) map.get("refresh_token");
                if (accessToken != null) {
                    if (expiresInSecs != null && refreshToken != null) {
                        final Profile profile = getProfile(accessToken);
                        return new OAuthAccount("", profile.getName(), profile.getEmail(), accessToken, computeExpiryTime(expiresInSecs), refreshToken, allowedScopes);
                    }
                    else {
                        throw new AuthorizationException("No expires_in or refresh_token. Response was:\n" + responseText);
                    }
                }
                else {
                    throw new AuthorizationException("Could not parse access_token, expires_in or refresh_token from received json '" + responseText + "'.");
                }
            }
            else {
                throw new AuthorizationException("The server returned code " + getResponseError(response));
            }
        }
        catch (IOException e) {
            throw new CommunicationException(e);
        }
    }

    @java.beans.Transient
    private HttpServer getHttpServer() throws IOException {
        // TODO Note: create throws a SocketException if already bound (and maybe when firewall refuses to open port)
        // TODO catch it and switch to copy/paste mode in that case
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT_GINJ), 0);
        server.createContext("/", httpExchange ->
        {
            // Check the response
            Map<String, String> params = queryToMap(httpExchange.getRequestURI().getQuery());
            final String error = params.get("error");
            final String code = params.get("code");
            final String scopeStr = params.get("scope");

            try {
                if (scopeStr != null) {
                    receivedScopes = new ArrayList<>(Arrays.asList(scopeStr.split(" "))); // Convert to Arraylist because Arrays.asList() cannot be persisted (no no-arg constructor)
                }

                checkScopesResponse(error, code, receivedScopes);
                // Send response
                sendTextResponse(httpExchange, HTML_BODY_OPEN + "<h1>Authorization received.</h1>"
                        + "<p>Congratulations! " + Ginj.getAppName() + " is now authorized to upload and share your captures on your " + getExporterName() + " account.<br/>"
                        + "You can revoke these authorizations at any time by visiting <a href=\"" + getOAuth2RevokeUrl() + "\">" + getOAuth2RevokeUrl() + "</a>.</p>"
                        + "<p>You may now close this Window.</p>" + BODY_HTML_CLOSE);
                if (receivedCode == null) {
                    // First callback. Remember the received code, which will continue at Step 5.
                    receivedCode = code;
                    // System.out.println("Server received code = " + code);
                }
            }
            catch (Exception e) {
                // Send response
                sendTextResponse(httpExchange, HTML_BODY_OPEN + "<h1>Authorization rejected.</h1>"
                        + "<p>" + Ginj.getAppName() + " did not receive the required authorizations to access your " + getExporterName() + " account.<br/>"
                        + "Operation cancelled.</p>"
                        + "<p>You may now close this Window.</p>" + BODY_HTML_CLOSE);
                cancel();
            }
        });
        return server;
    }

    private void sendTextResponse(HttpExchange httpExchange, String responseStr) throws IOException {
        byte[] response = responseStr.getBytes(UTF_8);
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, response.length);
        OutputStream out = httpExchange.getResponseBody();
        out.write(response);
        out.close();
    }

    private void checkScopesResponse(String error, String code, List<String> allowedScopes) throws AuthorizationException {
        if (error != null) {
            throw new AuthorizationException(getExporterName() + " returned an error: " + error);
        }
        if (code == null || code.isEmpty()) {
            throw new AuthorizationException("Missing code (" + code + ") in " + getExporterName() + " response.");
        }
        if (getRequiredScopes() != null) {
            if (allowedScopes == null || allowedScopes.isEmpty()) {
                throw new AuthorizationException("No allowed scope received in " + getExporterName() + " response.");
            }
            List<String> missingScopes = getMissingScopes(allowedScopes);
            if (missingScopes.isEmpty()) {
                return;
            }
            throw new AuthorizationException("The following " + getExporterName() + " authorizations are missing: " + missingScopes);
        }
    }

    @java.beans.Transient
    protected List<String> getMissingScopes(String scopeStr) {
        return getMissingScopes(Arrays.asList(scopeStr.split(" ")));
    }

    @java.beans.Transient
    protected List<String> getMissingScopes(List<String> acceptedScopes) {
        List<String> missingScopes = new ArrayList<>();
        for (String requiredScope : getRequiredScopes()) {
            if (!acceptedScopes.contains(requiredScope)) {
                missingScopes.add(requiredScope);
            }
        }
        return missingScopes;
    }

    @java.beans.Transient
    public String getAccessToken(Account account) throws AuthorizationException {

        String accessToken = ((OAuthAccount)account).getAccessToken();
        Date accessExpiry = ((OAuthAccount)account).getAccessExpiry();
        if (accessToken == null || accessToken.isBlank() || accessExpiry == null ) {
            throw new AuthorizationException("No previous information found in preferences");
        }

        // Let's take a 1-minute security margin
        Calendar inOneMinute = Calendar.getInstance();
        inOneMinute.add(Calendar.MINUTE, 1);
        if (inOneMinute.getTime().after(accessExpiry)) {
            // Token is expired (or will be in 1 minute). Ask a new one
            accessToken = refreshAccessToken(account);
        }
        // Return access token
        return accessToken;
    }

    /**
     * Implements e.g. https://developers.google.com/identity/protocols/oauth2/native-app#offline
     * Note: if server responds with Error 400 invalid_grant, a list of possible reasons is at
     * https://blog.timekit.io/google-oauth-invalid-grant-nightmare-and-how-to-fix-it-9f4efaf1da35
     */
    private String refreshAccessToken(Account account) throws AuthorizationException {
        CloseableHttpClient client = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost(getOAuth2TokenUrl());

        String refreshToken = ((OAuthAccount)account).getRefreshToken();

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthorizationException();
        }

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("client_id", getClientAppId()));
        params.add(new BasicNameValuePair("grant_type", "refresh_token"));
        params.add(new BasicNameValuePair("refresh_token", refreshToken));
        if (getSecretAppKey() != null) {
            params.add(new BasicNameValuePair("client_secret", getSecretAppKey()));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(params));

        try {
            CloseableHttpResponse response = client.execute(httpPost);
            if (isStatusOK(response.getCode())) {
                final String responseText;
                try {
                    responseText = EntityUtils.toString(response.getEntity());
                }
                catch (ParseException e) {
                    throw new AuthorizationException("Could not parse token refresh response as String: " + response.getEntity());
                }
                @SuppressWarnings("rawtypes")
                Map map = new Gson().fromJson(responseText, Map.class);
                String accessToken = (String) map.get("access_token");
                Double expiresInSecs = (Double) map.get("expires_in");
                String scopeStr = (String) map.get("scope");

                if (scopeStr != null) { // e.g. Dropbox does not currently return scopes
                    // Check scopes
                    List<String> missingScopes = getMissingScopes(scopeStr);
                    if (!missingScopes.isEmpty()) {
                        throw new AuthorizationException("The following authorizations are missing: " + missingScopes + ". Please re-authorize this account.");
                    }
                }
                if (accessToken != null && expiresInSecs != null) {
                    ((OAuthAccount)account).setAccessToken(accessToken);
                    ((OAuthAccount)account).setAccessExpiry(computeExpiryTime(expiresInSecs));
                    Ginj.getTargetPrefs().save();

                    return accessToken;
                }
                else {
                    throw new AuthorizationException("Could not parse access_token or expires_in from received json '" + responseText + "'.");
                }
            }
            else {
                // throw new AuthorizationException("The server returned code " + getResponseError(response));
                // This code used to track a nasty bug and throw additional info in that case... I'm leaving it.
                final int code = response.getCode();
                String responseText = null;
                try {
                    responseText = EntityUtils.toString(response.getEntity());
                }
                catch (IOException | ParseException e) {
                    // noop
                }

                // Fine tune the error message
                if (responseText == null) {
                    throw new AuthorizationException("The server returned code " + code);
                }
                else {
                    if (code == 400) {
                        // Bad request. Typical error:
                        //{
                        //    "error": "invalid_grant",
                        //    "error_description": "Token has been expired or revoked."
                        //}
                        @SuppressWarnings("rawtypes")
                        Map map = new Gson().fromJson(responseText, Map.class);
                        String error = (String) map.get("error");
                        String errorDescription = (String) map.get("error_description");
                        if ("invalid_grant".equals(error) && "Token has been expired or revoked.".equals(errorDescription)) {
                            // Well known (and unexplained) error.
                            // Remove stored tokens to force a reauthorization
                            clearOAuthTokens((OAuthAccount)account);
                            throw new AuthorizationException("Google refuses to refresh the access token. Clearing previous refresh_token to force re-authorize...\nError was: " + code + " (" + responseText + ")");
                        }
                    }

                    throw new AuthorizationException("The server returned code " + code + " (" + responseText + ")");
                }

            }
        }
        catch (IOException e) {
            throw new AuthorizationException(e);
        }
    }

    private Date computeExpiryTime(Double expiresInSecs) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, expiresInSecs.intValue());
        return calendar.getTime();
    }

    protected void clearOAuthTokens(OAuthAccount account) {
        account.setAccessToken(null);
        account.setAccessExpiry(null);
        account.setRefreshToken(null);
        Ginj.getTargetPrefs().save();
    }

    public void cancel() {
        super.cancel();
        try {
            if (server != null) {
                server.stop(0);
            }
        }
        catch (Exception e) {
            // ignore
        }
    }


    /////////////////////////
    // Utils


    private static String encodeScopes(List<String> requiredScopes) {
        StringBuilder concat = new StringBuilder();
        for (String requiredScope : requiredScopes) {
            if (concat.length() > 0) {
                concat.append(" ");
            }
            concat.append(requiredScope);
        }
        return URLEncoder.encode(concat.toString(), UTF_8);
    }

    public static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            }
            else {
                result.put(entry[0], "");
            }
        }
        return result;
    }

    protected static boolean isStatusOK(int code) {
        return code >= 200 && code < 300;
    }

    @java.beans.Transient
    protected String getResponseError(CloseableHttpResponse httpResponse) {
        String errorMsg = String.valueOf(httpResponse.getCode());
        try {
            errorMsg += " (" + EntityUtils.toString(httpResponse.getEntity()) + ")";
            // Note: if error is 405 and message complains it expected POST and received GET, it could be that the URL was redirected (301), eg from http to https
            // Which httpComponents by default converts from POST to GET
            // Seems the default has changed from HttpComponents 4 to 5 : https://www.baeldung.com/httpclient-redirect-on-http-post
        }
        catch (IOException | ParseException e) {
            // noop
        }
        return errorMsg;
    }

    @java.beans.Transient
    protected abstract Profile getProfile(String accessToken) throws CommunicationException, AuthorizationException;

}
