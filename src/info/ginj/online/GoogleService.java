package info.ginj.online;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import info.ginj.Ginj;
import info.ginj.Prefs;
import info.ginj.online.exception.AuthorizationException;
import info.ginj.online.exception.CommunicationException;
import info.ginj.ui.Util;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

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
public abstract class GoogleService {
    public static final int PORT_GINJ = 6193;
    public static final String HTML_BODY_OPEN = "<html><head><style>body{background-color:" + Util.colorToHex(Util.LABEL_BACKGROUND_COLOR) + ";font-family:sans-serif;color:" + Util.colorToHex(Util.LABEL_FOREGROUND_COLOR) + ";} a{color:" + Util.colorToHex(Util.ICON_ENABLED_COLOR) + ";} a:hover{color:white;}</style></head><body>";
    public static final String BODY_HTML_CLOSE = "</body></html>";
    public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd_HH_mm_ss";
    private static final String GOOGLE_CLIENT_APP_ID = "805469689820-c3drai5blocq5ae120md067te73ejv49.apps.googleusercontent.com";
    private static final String GOOGLE_NOT_SO_SECRET_CLIENT_APP_KEY = "2guKmYBdrb1nhGkMgdSrbeXl"; // "In this context, the client secret is obviously not treated as a secret." ( https://developers.google.com/identity/protocols/oauth2 )
    private static String verifier;
    private static String receivedCode = null;
    private static boolean abortRequested = false;
    HttpServer server;

    public abstract String getServiceName();

    protected abstract String[] getRequiredScopes();

    protected abstract Prefs.Key getRefreshTokenKeyPrefix();

    protected abstract Prefs.Key getAccessTokenKeyPrefix();

    protected abstract Prefs.Key getAccessExpiryKeyPrefix();

    public void authorize(String accountNumber) throws AuthorizationException {
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
            verifier = encoder.encodeToString(code);
            //System.out.println("verifier = " + verifier);

            // Create a Code Challenge
            byte[] bytes = verifier.getBytes(StandardCharsets.US_ASCII);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(bytes, 0, bytes.length);
            byte[] digest = md.digest();
            final String challenge = encoder.encodeToString(digest).replaceAll("=+$", "");
            //System.out.println("challenge = " + challenge);

            // Prepare the URL to forward the user to
            String url = "https://accounts.google.com/o/oauth2/v2/auth";
            url += "?client_id=" + GOOGLE_CLIENT_APP_ID;
            url += "&response_type=code";
            url += "&code_challenge=" + challenge;
            url += "&code_challenge_method=S256";
            url += "&scope=" + GoogleService.encodeScopes(getRequiredScopes());
            url += "&redirect_uri=" + URLEncoder.encode("http://127.0.0.1:" + PORT_GINJ + "/google", UTF_8); // for local server
            // url += "&redirect_uri=" + URLEncoder.encode("urn:ietf:wg:oauth:2.0:oob", UTF_8); // for Copy/paste.
            //System.out.println(url);

            // Step 2: Send a request to Google's OAuth 2.0 server

            // Open that page in the user's default browser
            Desktop.getDesktop().browse(new URI(url));

            // (Step 3: Google prompts user for consent)

            // Step 4: Handle the OAuth 2.0 server response (see async http server code)

            // Wait for code to be received by our http server...
            long timeOutTime = System.currentTimeMillis() + 5 * 60 * 1000;
            while (receivedCode == null && System.currentTimeMillis() < timeOutTime && !abortRequested) {
                //noinspection BusyWait
                Thread.sleep(100);
            }
            // When we get here, it's because of either abort requested, response received, or time-out
            if (!abortRequested) {
                if (receivedCode != null) {
                    // Step 5: Exchange authorization code for refresh and access tokens
                    exchangeCodeForTokens(receivedCode, accountNumber);
                }
                else {
                    // time-out
                    throw new AuthorizationException("Time out waiting for authorization");
                }
            }
        }
        catch (NoSuchAlgorithmException | URISyntaxException | IOException | InterruptedException e) {
            throw new AuthorizationException(e);
        }
        finally {
            if (server != null) {
                // Shutdown server
                server.stop(2);
            }
        }
    }

    public void abortAuthorization() {
        abortRequested = true;
        try {
            if (server != null) {
                server.stop(0);
            }
        }
        catch (Exception e) {
            // ignore
        }
    }

    public void checkAuthorizations(String accountNumber) throws CommunicationException, AuthorizationException {
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
                    clearGoogleTokens(accountNumber);
                    throw new AuthorizationException(msg + ". Please re-authorize...");
                }

                if (scopeStr == null || scopeStr.isBlank()) {
                    // Remove stored tokens to force a reauthorization
                    clearGoogleTokens(accountNumber);
                    throw new AuthorizationException("No scope is defined for this token. Please re-authorize...");
                }

                final List<String> missingScopes = getMissingScopes(scopeStr);
                if (!missingScopes.isEmpty()) {
                    throw new AuthorizationException("The authorizations below are missing for " + getServiceName() + ". Please re-authorize this account.\n Missing scopes: " + missingScopes);
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

    private void exchangeCodeForTokens(String code, String accountNumber) throws AuthorizationException {
        CloseableHttpClient client = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost("https://oauth2.googleapis.com/token");

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("client_id", GOOGLE_CLIENT_APP_ID));
        params.add(new BasicNameValuePair("client_secret", GOOGLE_NOT_SO_SECRET_CLIENT_APP_KEY));
        params.add(new BasicNameValuePair("code", code));
        params.add(new BasicNameValuePair("code_verifier", verifier));
        params.add(new BasicNameValuePair("grant_type", "authorization_code"));
        params.add(new BasicNameValuePair("redirect_uri", "http://127.0.0.1:" + PORT_GINJ + "/google")); // What's the use ?
        httpPost.setEntity(new UrlEncodedFormEntity(params));

        try {
            CloseableHttpResponse response = client.execute(httpPost);

            if (GoogleService.isStatusOK(response.getCode())) {
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
                Double expiresIn = (Double) map.get("expires_in");
                String refreshToken = (String) map.get("refresh_token");

                if (accessToken != null && expiresIn != null && refreshToken != null) {

                    LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(expiresIn.longValue());
                    String expiryTimeStr = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN).format(expiryTime);

                    Prefs.setWithSuffix(getAccessTokenKeyPrefix(), accountNumber, accessToken);
                    Prefs.setWithSuffix(getAccessExpiryKeyPrefix(), accountNumber, expiryTimeStr);
                    Prefs.setWithSuffix(getRefreshTokenKeyPrefix(), accountNumber, refreshToken);
                    Prefs.save();
                }
                else {
                    throw new AuthorizationException("Could not parse access_token, expires_in or refresh_token from received json '" + responseText + "'.");
                }
            }
            else {
                throw new AuthorizationException("Server returned code " + getResponseError(response));
            }
        }
        catch (IOException e) {
            throw new AuthorizationException(e);
        }
    }

    public String getAccessToken(String accountNumber) throws AuthorizationException {
        String accessToken = Prefs.getWithSuffix(getAccessTokenKeyPrefix(), accountNumber);
        String expiryStr = Prefs.getWithSuffix(getAccessExpiryKeyPrefix(), accountNumber);
        if (accessToken == null || expiryStr == null || accessToken.isBlank() || expiryStr.isBlank()) {
            throw new AuthorizationException("No previous information found in preferences");
        }

        // Let's take a 1-minute security margin
        String nowStr = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN).format(LocalDateTime.now().plusMinutes(1));
        if (nowStr.compareTo(expiryStr) > 0) {
            // Token is expired (or will be in 1 minute). Ask a new one
            accessToken = refreshAccessToken(accountNumber);
        }
        // Return access token
        return accessToken;
    }

    /**
     * Implements https://developers.google.com/identity/protocols/oauth2/native-app#offline
     * Note: if server responds with Error 400 invalid_grant, a list of possible reasons is at
     * https://blog.timekit.io/google-oauth-invalid-grant-nightmare-and-how-to-fix-it-9f4efaf1da35
     */
    private String refreshAccessToken(String accountNumber) throws AuthorizationException {
        CloseableHttpClient client = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost("https://oauth2.googleapis.com/token");

        final String refreshToken = Prefs.getWithSuffix(getRefreshTokenKeyPrefix(), accountNumber);

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthorizationException();
        }

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("client_id", GOOGLE_CLIENT_APP_ID));
        params.add(new BasicNameValuePair("client_secret", GOOGLE_NOT_SO_SECRET_CLIENT_APP_KEY));
        params.add(new BasicNameValuePair("grant_type", "refresh_token"));
        params.add(new BasicNameValuePair("refresh_token", refreshToken));
        httpPost.setEntity(new UrlEncodedFormEntity(params));

        try {
            CloseableHttpResponse response = client.execute(httpPost);
            if (GoogleService.isStatusOK(response.getCode())) {
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
                Double expiresIn = (Double) map.get("expires_in");
                String scopeStr = (String) map.get("scope");

                // Check scopes
                List<String> missingScopes = getMissingScopes(scopeStr);
                if (!missingScopes.isEmpty()) {
                    throw new AuthorizationException("The following authorizations are missing: " + missingScopes + ". Please re-authorize this account.");
                }

                if (accessToken != null && expiresIn != null) {
                    LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(expiresIn.longValue());
                    String expiryTimeStr = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN).format(expiryTime);

                    Prefs.setWithSuffix(getAccessTokenKeyPrefix(), accountNumber, accessToken);
                    Prefs.setWithSuffix(getAccessExpiryKeyPrefix(), accountNumber, expiryTimeStr);
                    Prefs.save();

                    return accessToken;
                }
                else {
                    throw new AuthorizationException("Could not parse access_token or expires_in from received json '" + responseText + "'.");
                }
            }
            else {
                // throw new AuthorizationException("Server returned code " + getResponseError(response));
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
                    throw new AuthorizationException("Server returned code " + code);
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
                        if ("invalid_grant".equals(error) && "Token has been expired or revoked.".equals(errorDescription)){
                            // Well known (and unexplained) error.
                            // Remove stored tokens to force a reauthorization
                            clearGoogleTokens(accountNumber);
                            throw new AuthorizationException("Google refuses to refresh the access token. Clearing previous refresh_token to force re-authorize...\nError was: " + code + " (" + responseText + ")");
                        }
                    }

                    throw new AuthorizationException("Server returned code " + code + " (" + responseText + ")");
                }

            }
        }
        catch (IOException e) {
            throw new AuthorizationException(e);
        }
    }

    private void clearGoogleTokens(String accountNumber) {
        Prefs.removeWithSuffix(getAccessTokenKeyPrefix(), accountNumber);
        Prefs.removeWithSuffix(getAccessExpiryKeyPrefix(), accountNumber);
        Prefs.removeWithSuffix(getRefreshTokenKeyPrefix(), accountNumber);
        Prefs.save();
    }

    private HttpServer getHttpServer() throws IOException {
        // TODO Note: create throws a SocketException if already bound (and maybe when firewall refuses to open port)
        // TODO catch it and switch to copy/paste mode in that case
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT_GINJ), 0);
        server.createContext("/google", httpExchange ->
        {
            // Check the response
            Map<String, String> params = GoogleService.queryToMap(httpExchange.getRequestURI().getQuery());
            final String error = params.get("error");
            final String code = params.get("code");
            final String scopes = params.get("scope");

            try {
                checkResponse(error, code, scopes);
                // Send response
                sendResponse(httpExchange, HTML_BODY_OPEN + "<h1>Authorization received.</h1>"
                        + "<p>Congratulations! " + Ginj.getAppName() + " is now authorized upload and share your captures on your " + getServiceName() + " account.<br/>"
                        + "You can revoke these authorizations at any time by visiting <a href=\"https://myaccount.google.com/permissions\">https://myaccount.google.com/permissions</a>.</p>"
                        + "<p>You may now close this Window.</p>" + BODY_HTML_CLOSE);
                if (receivedCode == null) {
                    // First callback. Remember the received code, which will continue at Step 5.
                    receivedCode = code;
                }
            }
            catch (Exception e) {
                // Send response
                sendResponse(httpExchange, HTML_BODY_OPEN + "<h1>Authorization rejected.</h1>"
                        + "<p>" + Ginj.getAppName() + " did not receive the required authorizations to access your " + getServiceName() + " account.<br/>"
                        + "Operation cancelled.</p>"
                        + "<p>You may now close this Window.</p>" + BODY_HTML_CLOSE);
                abortAuthorization();
            }
        });
        return server;
    }

    // Hi-level utils

    private void sendResponse(HttpExchange httpExchange, String responseStr) throws IOException {
        byte[] response = responseStr.getBytes(UTF_8);
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, response.length);
        OutputStream out = httpExchange.getResponseBody();
        out.write(response);
        out.close();
    }

    private void checkResponse(String error, String code, String scopeStr) throws AuthorizationException {
        if (error != null) {
            throw new AuthorizationException(getServiceName() + " returned an error: " + error);
        }
        if (code == null || code.isEmpty() || scopeStr == null || scopeStr.isBlank()) {
            throw new AuthorizationException("Missing code (" + code + ") or scope (" + scopeStr + ") in " + getServiceName() + " response.");
        }
        List<String> missingScopes = getMissingScopes(scopeStr);
        if (missingScopes.isEmpty()) {
            return;
        }
        throw new AuthorizationException("The following authorizations are missing: " + missingScopes);
    }

    protected List<String> getMissingScopes(String scopeStr) {
        final List<String> acceptedScopes = Arrays.asList(scopeStr.split(" "));
        List<String> missingScopes = new ArrayList<>();
        for (String requiredScope : getRequiredScopes()) {
            if (!acceptedScopes.contains(requiredScope)) {
                missingScopes.add(requiredScope);
            }
        }
        return missingScopes;
    }

    // Low level utils

    protected static boolean isStatusOK(int code) {
        return code >= 200 && code < 300;
    }

    protected static String getResponseError(CloseableHttpResponse response) {
        String errorMsg = String.valueOf(response.getCode());
        try {
            errorMsg += " (" + EntityUtils.toString(response.getEntity()) + ")";
        }
        catch (IOException | ParseException e) {
            // noop
        }
        return errorMsg;
    }

    private static String encodeScopes(String[] requiredScopes) {
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

}
