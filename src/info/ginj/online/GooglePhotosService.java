package info.ginj.online;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import info.ginj.Capture;
import info.ginj.Ginj;
import info.ginj.Prefs;
import info.ginj.online.exception.AuthorizationException;
import info.ginj.online.exception.CommunicationException;
import info.ginj.online.exception.UploadException;
import info.ginj.ui.Util;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.FileEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.awt.*;
import java.io.File;
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
 * Handles interaction with Google Photos service
 * OAuth2 authorization flow based on
 * https://developers.google.com/identity/protocols/oauth2
 * and
 * https://developers.google.com/identity/protocols/oauth2/native-app#obtainingaccesstokens
 * See also
 * https://developers.google.com/photos/library/guides/authorization
 * <p>
 * TODO: when creating account, remember to tell user that Ginj medias are uploaded in full quality and will count in the user quota
 * TODO: videos must be max 10GB
 * TODO: only keep a single HttpClient ?
 */
public class GooglePhotosService implements OnlineService {
    public static final int PORT_GINJ = 6193;

    private static final String GOOGLE_CLIENT_APP_ID = "805469689820-c3drai5blocq5ae120md067te73ejv49.apps.googleusercontent.com";
    private static final String GOOGLE_NOT_SO_SECRET_CLIENT_APP_KEY = "2guKmYBdrb1nhGkMgdSrbeXl"; // "In this context, the client secret is obviously not treated as a secret." ( https://developers.google.com/identity/protocols/oauth2 )

    // "Access to create an album, share it, upload media items to it, and join a shared album."
    private static final String[] GOOGLE_PHOTOS_REQUIRED_SCOPES = {"https://www.googleapis.com/auth/photoslibrary.appendonly", "https://www.googleapis.com/auth/photoslibrary.readonly.appcreateddata", "https://www.googleapis.com/auth/photoslibrary.sharing"};
    private static final String[] YOUTUBE_REQUIRED_SCOPES = {"https://www.googleapis.com/auth/youtube.upload"};
    private static final String[] GOOGLE_DRIVE_REQUIRED_SCOPE = {"https://www.googleapis.com/auth/drive"};// remove trailing equal;

    public static final String HTML_BODY_OPEN = "<html><head><style>body{background-color:" + Util.colorToHex(Util.LABEL_BACKGROUND_COLOR) + ";font-family:sans-serif;color:" + Util.colorToHex(Util.LABEL_FOREGROUND_COLOR) + ";} a{color:" + Util.colorToHex(Util.ICON_ENABLED_COLOR) + ";} a:hover{color:white;}</style></head><body>";
    public static final String BODY_HTML_CLOSE = "</body></html>";
    public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd_HH_mm_ss";

    private static String verifier;
    private static String receivedCode = null;
    private static boolean abortRequested = false;

    HttpServer server;

    @Override
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
            url += "&scope=" + encodeScopes(GOOGLE_PHOTOS_REQUIRED_SCOPES);
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

/*
        Upon API calls:
        You can provide an OAuth 2.0 token in either of the following ways:
        Use the access_token query parameter like this: ?access_token=oauth2-token
        Use the HTTP Authorization header like this: Authorization: Bearer oauth2-token
 */
    }

    private HttpServer getHttpServer() throws IOException {
        // TODO Note: create throws a SocketException if already bound (and maybe when firewall refuses to open port)
        // TODO catch it and switch to copy/paste mode in that case
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT_GINJ), 0);
        server.createContext("/google", httpExchange ->
        {
            // Check the response
            Map<String, String> params = queryToMap(httpExchange.getRequestURI().getQuery());
            final String error = params.get("error");
            final String code = params.get("code");
            final String scopes = params.get("scope");

            try {
                checkResponse(error, code, scopes);
                // Send response
                sendResponse(httpExchange, HTML_BODY_OPEN + "<h1>Authorization received.</h1>"
                        + "<p>Congratulations! " + Ginj.getAppName() + " is now authorized upload and share your captures on your Google Photos account.<br/>"
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
                        + "<p>" + Ginj.getAppName() + " did not receive the required authorizations to access your Google Photos account.<br/>"
                        + "Operation cancelled.</p>"
                        + "<p>You may now close this Window.</p>" + BODY_HTML_CLOSE);
                abortAuthorization();
            }
        });
        return server;
    }

    public String getAccessToken(String accountNumber) throws AuthorizationException {
        String accessToken = Prefs.getWithSuffix(Prefs.Key.EXPORTER_GOOGLE_PHOTOS_ACCESS_TOKEN_PREFIX, accountNumber);
        String expiryStr = Prefs.getWithSuffix(Prefs.Key.EXPORTER_GOOGLE_PHOTOS_ACCESS_EXPIRY_PREFIX, accountNumber);
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

    private void sendResponse(HttpExchange httpExchange, String responseStr) throws IOException {
        byte[] response = responseStr.getBytes(UTF_8);
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, response.length);
        OutputStream out = httpExchange.getResponseBody();
        out.write(response);
        out.close();
    }

    private static void exchangeCodeForTokens(String code, String accountNumber) throws AuthorizationException {
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

            if (isStatusOK(response.getCode())) {
                final String responseText;
                try {
                    responseText = EntityUtils.toString(response.getEntity());
                }
                catch (ParseException e) {
                    throw new AuthorizationException("Could not parse server response as String: " + response.getEntity());
                }
                Map map = new Gson().fromJson(responseText, Map.class);
                String accessToken = (String) map.get("access_token");
                Double expiresIn = (Double) map.get("expires_in");
                String refreshToken = (String) map.get("refresh_token");

                if (accessToken != null && expiresIn != null && refreshToken != null) {

                    LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(expiresIn.longValue());
                    String expiryTimeStr = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN).format(expiryTime);

                    Prefs.setWithSuffix(Prefs.Key.EXPORTER_GOOGLE_PHOTOS_ACCESS_TOKEN_PREFIX, accountNumber, accessToken);
                    Prefs.setWithSuffix(Prefs.Key.EXPORTER_GOOGLE_PHOTOS_ACCESS_EXPIRY_PREFIX, accountNumber, expiryTimeStr);
                    Prefs.setWithSuffix(Prefs.Key.EXPORTER_GOOGLE_PHOTOS_REFRESH_TOKEN_PREFIX, accountNumber, accessToken);
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

    /**
     * Implements https://developers.google.com/identity/protocols/oauth2/native-app#offline
     */
    private static String refreshAccessToken(String accountNumber) throws AuthorizationException {
        CloseableHttpClient client = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost("https://oauth2.googleapis.com/token");

        final String refreshToken = Prefs.getWithSuffix(Prefs.Key.EXPORTER_GOOGLE_PHOTOS_REFRESH_TOKEN_PREFIX, accountNumber);

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
            if (isStatusOK(response.getCode())) {
                final String responseText;
                try {
                    responseText = EntityUtils.toString(response.getEntity());
                }
                catch (ParseException e) {
                    throw new AuthorizationException("Could not parse token refresh response as String: " + response.getEntity());
                }
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

                    Prefs.setWithSuffix(Prefs.Key.EXPORTER_GOOGLE_PHOTOS_ACCESS_TOKEN_PREFIX, accountNumber, accessToken);
                    Prefs.setWithSuffix(Prefs.Key.EXPORTER_GOOGLE_PHOTOS_ACCESS_EXPIRY_PREFIX, accountNumber, expiryTimeStr);
                    Prefs.save();

                    return accessToken;
                }
                else {
                    throw new AuthorizationException("Could not parse access_token or expires_in from received json '" + responseText + "'.");
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

    private static boolean isStatusOK(int code) {
        return code >= 200 && code < 300;
    }

    @Override
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

    @Override
    public void uploadCapture(Capture capture, String accountNumber) throws AuthorizationException, UploadException, CommunicationException {
        // We need an actual file (for now at least)
        final File file;
        try {
            file = capture.toFile();
        }
        catch (IOException e) {
            throw new UploadException("Error preparing file to upload", e);
        }

        // Step 1: Retrieve Ginj album ID, or create it if needed
        final String albumId = getGinjAlbumId(accountNumber);

        // Step 2: Upload bytes
        final String uploadToken = uploadFileBytes(file, accountNumber);

        // Step 3: Create a media item in a Ginj album
        String mediaId = createMediaItem(capture, accountNumber, albumId, uploadToken);

        // Step 4: Share the album (one cannot share a single media using the API) and return its link

        //TODO


    }

    private String createMediaItem(Capture capture, String accountNumber, String albumId, String uploadToken) throws AuthorizationException, UploadException {
        CloseableHttpClient client = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost("https://photoslibrary.googleapis.com/v1/mediaItems:batchCreate");

        httpPost.addHeader("Authorization", "Bearer " + getAccessToken(accountNumber));
        httpPost.addHeader("Content-type", "application/json");

        // Build JSon query:
        JsonObject simpleMediaItem = new JsonObject();
        simpleMediaItem.addProperty("fileName", capture.getDefaultName());
        simpleMediaItem.addProperty("uploadToken", uploadToken);

        JsonObject newMediaItem = new JsonObject();
        newMediaItem.addProperty("description", capture.getName());
        newMediaItem.add("simpleMediaItem", simpleMediaItem);

        JsonArray newMediaItems = new JsonArray();
        newMediaItems.add(newMediaItem);

        JsonObject json = new JsonObject();
        json.addProperty("albumId", albumId);
        json.add("newMediaItems", newMediaItems);

        String jsonString = new Gson().toJson(json);

        httpPost.setEntity(new StringEntity(jsonString));

        try {
            CloseableHttpResponse response = client.execute(httpPost);
            if (isStatusOK(response.getCode())) {
                final String responseText;
                try {
                    responseText = EntityUtils.toString(response.getEntity());
                }
                catch (ParseException e) {
                    throw new AuthorizationException("Could not parse media creation response as String: " + response.getEntity());
                }

                MediaCreationResponse mediaCreationResponse = new Gson().fromJson(responseText, MediaCreationResponse.class);
                if (mediaCreationResponse.getNewMediaItemResults().size() != 1) {
                    throw new UploadException("Media creation failed. Full response was '" + responseText + "'");
                }
                NewMediaItemResult mediaItemResult = mediaCreationResponse.getNewMediaItemResults().get(0);
                if (!"Success".equals(mediaItemResult.getStatus().getMessage())) {
                    throw new UploadException("Media creation failed. Full response was '" + responseText + "'");
                }
                mediaItemResult.getMediaItem().getProductUrl();
                return mediaItemResult.getMediaItem().getId();
            }
            else {
                throw new UploadException("Server returned code " + getResponseError(response) + " when creating media");
            }
        }
        catch (IOException e) {
            throw new UploadException("Error creating media", e);
        }
    }

    /**
     * Retrieves the Ginj album id, or create it if needed
     *
     * @param accountNumber
     * @return
     */
    private String getGinjAlbumId(String accountNumber) throws AuthorizationException, CommunicationException {
        String albumId = Prefs.getWithSuffix(Prefs.Key.EXPORTER_GOOGLE_PHOTOS_JING_ALBUM_ID_PREFIX, accountNumber);
        if (albumId == null || albumId.isBlank()) {

            // Try to find application album in the list of existing albums

            CloseableHttpClient client = HttpClients.createDefault();
            String pageToken = null;
            List<Album> albums = new ArrayList<>();

            do {
                pageToken = getNextAlbumPage(accountNumber, client, pageToken, albums);
            }
            while (pageToken != null);

            for (Album album : albums) {
                if (Ginj.getAppName().equals(album.title)) {
                    System.out.println("FOUND! " + album.id + " : " + album.title);
                    albumId = album.title;
                }
                else {
                    System.out.println("(" + album.id + " : " + album.title + ")");
                }
            }

            if (albumId == null || albumId.isBlank()) {
                // Not found. Create it
                albumId = createGinjAlbum(accountNumber, client);
            }

            // remember album id
            Prefs.setWithSuffix(Prefs.Key.EXPORTER_GOOGLE_PHOTOS_JING_ALBUM_ID_PREFIX, accountNumber, albumId);
            Prefs.save();
        }
        return albumId;
    }

    private String createGinjAlbum(String accountNumber, CloseableHttpClient client) throws AuthorizationException, CommunicationException {
        HttpPost httpPost = new HttpPost("https://photoslibrary.googleapis.com/v1/albums");

        httpPost.addHeader("Authorization", "Bearer " + getAccessToken(accountNumber));
        httpPost.addHeader("Content-type", "application/json");

        Album album = new Album();
        album.setTitle(Ginj.getAppName());

        final Gson gson = new Gson();
        JsonObject json = new JsonObject();
        json.add("album", gson.toJsonTree(album));

        String jsonString = gson.toJson(json);

        httpPost.setEntity(new StringEntity(jsonString));

        try {
            CloseableHttpResponse response = client.execute(httpPost);
            if (isStatusOK(response.getCode())) {
                final String responseText;
                try {
                    responseText = EntityUtils.toString(response.getEntity());
                }
                catch (ParseException e) {
                    throw new CommunicationException("Could not parse album creation response as String: " + response.getEntity());
                }
                // Parse response back
                album = gson.fromJson(responseText, Album.class);
                return album.id;
            }
            else {
                throw new CommunicationException("Server returned code " + getResponseError(response) + " when creating album");
            }
        }
        catch (IOException e) {
            throw new CommunicationException(e);
        }
    }

    private String getNextAlbumPage(String accountNumber, CloseableHttpClient client, String pageToken, List<Album> albums) throws AuthorizationException, CommunicationException {
        HttpGet httpGet;
        try {
            URIBuilder builder = new URIBuilder("https://photoslibrary.googleapis.com/v1/albums");
            builder.setParameter("excludeNonAppCreatedData", String.valueOf(false)); // optional, default is false  TODO set true
            builder.setParameter("pageSize", "10"); // optional, default is 20 TODO leave default
            if (pageToken != null) builder.setParameter("pageToken", pageToken); // Needed to scroll f
            httpGet = new HttpGet(builder.build());
        }
        catch (URISyntaxException e) {
            throw new CommunicationException(e);
        }

        httpGet.addHeader("Authorization", "Bearer " + getAccessToken(accountNumber));
        httpGet.addHeader("Content-type", "application/json");

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
                AlbumList albumList = new Gson().fromJson(responseText, AlbumList.class);
                if (albumList.albums != null) {
                    albums.addAll(albumList.albums);
                }
                return albumList.getNextPageToken();
            }
            else {
                throw new CommunicationException("Server returned code " + getResponseError(response) + " when listing albums");
            }
        }
        catch (IOException e) {
            throw new CommunicationException(e);
        }
    }

    private static String getResponseError(CloseableHttpResponse response) {
        String errorMsg = String.valueOf(response.getCode());
        try {
            errorMsg += " (" + EntityUtils.toString(response.getEntity()) + ")";
        }
        catch (IOException | ParseException e) {
            // noop
        }
        return errorMsg;
    }

    private String uploadFileBytes(File file, String accountNumber) throws AuthorizationException, UploadException {
        String uploadToken;
        CloseableHttpClient client = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost("https://photoslibrary.googleapis.com/v1/uploads");

        httpPost.addHeader("Authorization", "Bearer " + getAccessToken(accountNumber));
        httpPost.addHeader("Content-type", "application/octet-stream");
        httpPost.addHeader("X-Goog-Upload-Content-Type", "mime-type");
        httpPost.addHeader("X-Goog-Upload-Protocol", "raw");

        httpPost.setEntity(new FileEntity(file, ContentType.APPLICATION_OCTET_STREAM));
        try {
            CloseableHttpResponse response = client.execute(httpPost);
            if (isStatusOK(response.getCode())) {
                try {
                    uploadToken = EntityUtils.toString(response.getEntity());
                }
                catch (ParseException e) {
                    throw new AuthorizationException("Could not parse media upload response as String: " + response.getEntity());
                }
            }
            else {
                throw new UploadException("Server returned code " + getResponseError(response) + " when uploading file contents");
            }
        }
        catch (IOException e) {
            throw new UploadException("Error uploading file contents", e);
        }
        return uploadToken;
    }

    @Override
    public void checkAuthorized(String accountNumber) throws CommunicationException, AuthorizationException {
        getGinjAlbumId(accountNumber);
    }


    private static void checkResponse(String error, String code, String scopeStr) throws AuthorizationException {
        if (error != null) {
            throw new AuthorizationException("Google Photos returned an error: " + error);
        }
        if (code == null || code.isEmpty() || scopeStr == null || scopeStr.isBlank()) {
            throw new AuthorizationException("Missing code (" + code + ") or scope (" + scopeStr + ") in Google Photos response.");
        }
        List<String> missingScopes = getMissingScopes(scopeStr);
        if (missingScopes.isEmpty()) {
            return;
        }
        throw new AuthorizationException("The following authorizations are missing: " + missingScopes);
    }

    private static List<String> getMissingScopes(String scopeStr) {
        final List<String> acceptedScopes = Arrays.asList(scopeStr.split(" "));
        List<String> missingScopes = new ArrayList<>();
        for (String requiredScope : GOOGLE_PHOTOS_REQUIRED_SCOPES) {
            if (!acceptedScopes.contains(requiredScope)) {
                missingScopes.add(requiredScope);
            }
        }
        return missingScopes;
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


    ////////////////////////////////////////////////////
    // Autogenerated pojos for complex Json parsing
    // Created by http://jsonschema2pojo.org
    ////////////////////////////////////////////////////

    public static class MediaCreationResponse {
        @SerializedName("newMediaItemResults")
        @Expose
        private List<NewMediaItemResult> newMediaItemResults = null;

        public List<NewMediaItemResult> getNewMediaItemResults() {
            return newMediaItemResults;
        }

        public MediaCreationResponse() {
        }

        public void setNewMediaItemResults(List<NewMediaItemResult> newMediaItemResults) {
            this.newMediaItemResults = newMediaItemResults;
        }

    }

    public static class MediaItem {
        @SerializedName("id")
        @Expose
        private String id;
        @SerializedName("description")
        @Expose
        private String description;
        @SerializedName("productUrl")
        @Expose
        private String productUrl;
        @SerializedName("mimeType")
        @Expose
        private String mimeType;
        @SerializedName("mediaMetadata")
        @Expose
        private MediaMetadata mediaMetadata;
        @SerializedName("filename")
        @Expose
        private String filename;

        public MediaItem() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getProductUrl() {
            return productUrl;
        }

        public void setProductUrl(String productUrl) {
            this.productUrl = productUrl;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public MediaMetadata getMediaMetadata() {
            return mediaMetadata;
        }

        public void setMediaMetadata(MediaMetadata mediaMetadata) {
            this.mediaMetadata = mediaMetadata;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

    }


    public static class MediaMetadata {
        @SerializedName("width")
        @Expose
        private String width;
        @SerializedName("height")
        @Expose
        private String height;
        @SerializedName("creationTime")
        @Expose
        private String creationTime;
        @SerializedName("photo")
        @Expose
        private Photo photo;

        public MediaMetadata() {
        }

        public String getWidth() {
            return width;
        }

        public void setWidth(String width) {
            this.width = width;
        }

        public String getHeight() {
            return height;
        }

        public void setHeight(String height) {
            this.height = height;
        }

        public String getCreationTime() {
            return creationTime;
        }

        public void setCreationTime(String creationTime) {
            this.creationTime = creationTime;
        }

        public Photo getPhoto() {
            return photo;
        }

        public void setPhoto(Photo photo) {
            this.photo = photo;
        }

    }

    public static class NewMediaItemResult {
        @SerializedName("uploadToken")
        @Expose
        private String uploadToken;
        @SerializedName("status")
        @Expose
        private Status status;
        @SerializedName("mediaItem")
        @Expose
        private MediaItem mediaItem;

        public NewMediaItemResult() {
        }

        public String getUploadToken() {
            return uploadToken;
        }

        public void setUploadToken(String uploadToken) {
            this.uploadToken = uploadToken;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public MediaItem getMediaItem() {
            return mediaItem;
        }

        public void setMediaItem(MediaItem mediaItem) {
            this.mediaItem = mediaItem;
        }

    }

    public static class Photo {
        public Photo() {
        }
    }

    public static class Status {
        @SerializedName("message")
        @Expose
        private String message;

        @SerializedName("code")
        @Expose
        private Integer code;

        public Status() {
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

    }

    ////////////////////
    // Album-related

    public static class Album {

        @SerializedName("id")
        @Expose
        private String id;
        @SerializedName("title")
        @Expose
        private String title;
        @SerializedName("productUrl")
        @Expose
        private String productUrl;
        @SerializedName("mediaItemsCount")
        @Expose
        private String mediaItemsCount;
        @SerializedName("coverPhotoBaseUrl")
        @Expose
        private String coverPhotoBaseUrl;
        @SerializedName("coverPhotoMediaItemId")
        @Expose
        private String coverPhotoMediaItemId;

        /**
         * No args constructor for use in serialization
         */
        public Album() {
        }

        /**
         * @param mediaItemsCount
         * @param coverPhotoMediaItemId
         * @param coverPhotoBaseUrl
         * @param id
         * @param title
         * @param productUrl
         */
        public Album(String id, String title, String productUrl, String mediaItemsCount, String coverPhotoBaseUrl, String coverPhotoMediaItemId) {
            super();
            this.id = id;
            this.title = title;
            this.productUrl = productUrl;
            this.mediaItemsCount = mediaItemsCount;
            this.coverPhotoBaseUrl = coverPhotoBaseUrl;
            this.coverPhotoMediaItemId = coverPhotoMediaItemId;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getProductUrl() {
            return productUrl;
        }

        public void setProductUrl(String productUrl) {
            this.productUrl = productUrl;
        }

        public String getMediaItemsCount() {
            return mediaItemsCount;
        }

        public void setMediaItemsCount(String mediaItemsCount) {
            this.mediaItemsCount = mediaItemsCount;
        }

        public String getCoverPhotoBaseUrl() {
            return coverPhotoBaseUrl;
        }

        public void setCoverPhotoBaseUrl(String coverPhotoBaseUrl) {
            this.coverPhotoBaseUrl = coverPhotoBaseUrl;
        }

        public String getCoverPhotoMediaItemId() {
            return coverPhotoMediaItemId;
        }

        public void setCoverPhotoMediaItemId(String coverPhotoMediaItemId) {
            this.coverPhotoMediaItemId = coverPhotoMediaItemId;
        }

    }


    public static class AlbumList {

        @SerializedName("albums")
        @Expose
        private List<Album> albums = null;
        @SerializedName("nextPageToken")
        @Expose
        private String nextPageToken;

        /**
         * No args constructor for use in serialization
         */
        public AlbumList() {
        }

        /**
         * @param albums
         * @param nextPageToken
         */
        public AlbumList(List<Album> albums, String nextPageToken) {
            super();
            this.albums = albums;
            this.nextPageToken = nextPageToken;
        }

        public List<Album> getAlbums() {
            return albums;
        }

        public void setAlbums(List<Album> albums) {
            this.albums = albums;
        }

        public String getNextPageToken() {
            return nextPageToken;
        }

        public void setNextPageToken(String nextPageToken) {
            this.nextPageToken = nextPageToken;
        }

    }
}
