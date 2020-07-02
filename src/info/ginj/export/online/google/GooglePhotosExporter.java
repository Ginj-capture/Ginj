package info.ginj.export.online.google;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import info.ginj.Capture;
import info.ginj.Ginj;
import info.ginj.Prefs;
import info.ginj.export.online.OnlineExporter;
import info.ginj.export.online.exception.AuthorizationException;
import info.ginj.export.online.exception.CommunicationException;
import info.ginj.export.online.exception.UploadException;
import info.ginj.ui.Util;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;

import javax.swing.*;
import java.io.*;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static info.ginj.Ginj.DATE_FORMAT_PATTERN;

/**
 * Handles interaction with Google Photos service
 * See also https://developers.google.com/photos/library/guides/authorization
 * <p>
 * TODO: when creating account, remember to tell user that Ginj medias are uploaded in full quality and will count in the user quota
 * TODO: videos must be max 10GB
 */
public class GooglePhotosExporter extends GoogleExporter implements OnlineExporter {

    // "Access to create an album, share it, upload media items to it, and join a shared album."
    private static final String[] GOOGLE_PHOTOS_REQUIRED_SCOPES = {"https://www.googleapis.com/auth/photoslibrary.appendonly", "https://www.googleapis.com/auth/photoslibrary.readonly.appcreateddata", "https://www.googleapis.com/auth/photoslibrary.sharing"};

    public static final ByteArrayEntity EMPTY_ENTITY = new ByteArrayEntity(new byte[]{}, ContentType.APPLICATION_OCTET_STREAM);

    public static final int CHUNK_SIZE = 262144; // 256k


    public GooglePhotosExporter(JFrame frame) {
        super(frame);
    }

    @Override
    public String getExporterName() {
        return "Google Photos";
    }

    @Override
    protected String[] getRequiredScopes() {
        return GOOGLE_PHOTOS_REQUIRED_SCOPES;
    }

    @Override
    protected Prefs.Key getAccessTokenKeyPrefix() {
        return Prefs.Key.EXPORTER_GOOGLE_PHOTOS_ACCESS_TOKEN_PREFIX;
    }

    @Override
    protected Prefs.Key getAccessExpiryKeyPrefix() {
        return Prefs.Key.EXPORTER_GOOGLE_PHOTOS_ACCESS_EXPIRY_PREFIX;
    }

    @Override
    protected Prefs.Key getRefreshTokenKeyPrefix() {
        return Prefs.Key.EXPORTER_GOOGLE_PHOTOS_REFRESH_TOKEN_PREFIX;
    }


    /**
     * Uploads the given capture to Google Photos
     * This method is run in its own thread and should not access the GUI directly. All interaction
     * should go through synchronized objects or be enclosed in a SwingUtilities.invokeLater() logic
     *
     * @param capture       the capture to export
     * @param accountNumber the accountNumber to export this capture to (if relevant)
     */
    @Override
    public void exportCapture(Capture capture, String accountNumber) {
        try {
            final String albumUrl = uploadCapture(capture, accountNumber);
            if (albumUrl != null) {
                copyTextToClipboard(albumUrl);
                capture.addExport(getExporterName(), albumUrl, null); // TODO store media Id. UploadCapture should return an Export object
                // Indicate export is complete.
                complete("Upload successful. A link to the album containing your capture was copied to the clipboard");
            }
        }
        catch (Exception e) {
            Util.alertException(getFrame(), getExporterName() + "Error", "There was an error exporting to " + getExporterName(), e);
            failed("Upload error");
        }
    }


    
    ///////////////////////////////////////
    // Low level


    /**
     * Upload a capture to the Google Photo service and return the URL of the shared album containing the item.
     * @param capture The object representing the captured screenshot or video
     * @param accountNumber the number of this account among Google Photos accounts
     * @return a public URL to share to give access to the uploaded media.
     * @throws AuthorizationException if user has no, or insufficient, authorizations, or if a token error occurs
     * @throws CommunicationException if an url, network or decoding error occurs
     * @throws UploadException if an upload-specfic error occurs
     */
    @Override
    public String uploadCapture(Capture capture, String accountNumber) throws AuthorizationException, UploadException, CommunicationException {
        // We need an actual file (for now at least). Make sure we have or create one
        try {
            capture.toFile();
        }
        catch (IOException e) {
            throw new UploadException("Error preparing file to upload", e);
        }

        final CloseableHttpClient client = HttpClients.createDefault();

        // Step 1: Retrieve Ginj album ID, or create it if needed
        // + Share the album (one cannot share a single media using the API)
        final Album album = getOrCreateAlbum(client, accountNumber, capture);

        // Step 2: Upload bytes
        final String uploadToken = uploadFileBytes(client, accountNumber, capture);

        // Step 3: Create a media item in the album
        @SuppressWarnings("unused")
        String mediaId = createMediaItem(client, accountNumber, capture, album.getId(), uploadToken);
        // Unfortunately, mediaId seems to be useless as we can only share the album...

        return album.getShareInfo().getShareableUrl();
    }

    /**
     * Retrieves the album to upload image to, or create it if needed.
     * Also checks the album is shared, or shares it if needed.
     *
     * @param client the {@link CloseableHttpClient}
     * @param accountNumber the number of this account among Google Photos accounts
     * @param capture The object representing the captured screenshot or video
     * @return the retrieved or created album
     * @throws AuthorizationException if user has no, or insufficient, authorizations, or if a token error occurs
     * @throws CommunicationException if an url, network or decoding error occurs
     */
    private Album getOrCreateAlbum(CloseableHttpClient client, String accountNumber, Capture capture) throws AuthorizationException, CommunicationException {
        // Determine target album accorging to "granularity" preference:
        // Single album / One per day / One per Ginj session / One per capture name / one per capture id
        String granularity = Prefs.getWithSuffix(Prefs.Key.EXPORTER_GOOGLE_PHOTOS_ALBUM_GRANULARITY, accountNumber, "APP");
        String albumName = Ginj.getAppName() + switch (granularity) {
            case "APP" -> "";
            case "DAY" -> "_" + DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN).format(LocalDateTime.now());
            case "SESSION" -> "_SESS_" + Ginj.getSession();
            case "NAME" -> "_" + capture.getName();
            case "CAPTURE" -> "_ID_" + capture.getId();
            default -> throw new IllegalStateException("Unexpected granularity: " + granularity);
        };

        // Try to find the album in the list of existing albums
        logProgress("Getting album", 6);
        Album album = getAlbumByName(client, accountNumber, albumName);

        // See if we found it
        if (album != null) {
            // Yes. See if it is shared
            if (album.getShareInfo() == null || album.getShareInfo().getShareableUrl() == null) {
                // No, share it
                logProgress("Sharing album", 8);
                shareAlbum(client, accountNumber, album);
            }
        }
        else {
            // Not found. Create it
            logProgress("Creating album", 5);
            album = createAlbum(client, accountNumber, albumName);

            // And, share it
            logProgress("Sharing album", 4);
            shareAlbum(client, accountNumber, album);
        }

        return album;
    }


    /**
     * List all application albums and return the one with the given name.
     *
     * @param client the {@link CloseableHttpClient}
     * @param accountNumber the number of this account among Google Photos accounts
     * @param albumName the album name to find
     * @return the album found, or null if not found
     * @throws AuthorizationException if user has no, or insufficient, authorizations, or if a token error occurs
     * @throws CommunicationException if an url, network or decoding error occurs
     */
    private Album getAlbumByName(CloseableHttpClient client, String accountNumber, String albumName) throws AuthorizationException, CommunicationException {
        AlbumList albumList = null;
        do {
            albumList = getNextAlbumPage(client, accountNumber, (albumList==null)?null:albumList.getNextPageToken());
            for (Album candidate : albumList.albums) {
                if (albumName.equals(candidate.title)) {
                    return candidate;
                }
            }
        }
        while (albumList.getNextPageToken() != null);

        // Not found
        return null;
    }

    /**
     * List a page of albums
     * Implements https://developers.google.com/photos/library/reference/rest/v1/albums/list
     * @param client the {@link CloseableHttpClient}
     * @param accountNumber the number of this account among Google Photos accounts
     * @param pageToken the token to access a given page in the iteration, or null to start
     * @return a page of albums
     * @throws AuthorizationException if user has no, or insufficient, authorizations, or if a token error occurs
     * @throws CommunicationException if an url, network or decoding error occurs
     */
    private AlbumList getNextAlbumPage(CloseableHttpClient client, String accountNumber, String pageToken) throws AuthorizationException, CommunicationException {
        HttpGet httpGet;
        try {
            URIBuilder builder = new URIBuilder("https://photoslibrary.googleapis.com/v1/albums");
            builder.setParameter("excludeNonAppCreatedData", String.valueOf(true)); // only list app-created albums (default=false)
            // builder.setParameter("pageSize", "10"); // (default is 20)
            if (pageToken != null) builder.setParameter("pageToken", pageToken);
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
                return new Gson().fromJson(responseText, AlbumList.class);
            }
            else {
                throw new CommunicationException("Server returned an error when listing albums: " + getResponseError(response));
            }
        }
        catch (IOException e) {
            throw new CommunicationException(e);
        }
    }

    /**
     * Shares the given album
     * Implements https://developers.google.com/photos/library/reference/rest/v1/albums/share
     * @param client the {@link CloseableHttpClient}
     * @param accountNumber the number of this account among Google Photos accounts
     * @param album the album to share. Will be modified to include ShareInfo once shared.
     * @throws AuthorizationException if user has no, or insufficient, authorizations, or if a token error occurs
     * @throws CommunicationException if an url, network or decoding error occurs
     */
    private void shareAlbum(CloseableHttpClient client, String accountNumber, Album album) throws AuthorizationException, CommunicationException {
        HttpPost httpPost = new HttpPost("https://photoslibrary.googleapis.com/v1/albums/" + album.id + ":share");

        httpPost.addHeader("Authorization", "Bearer " + getAccessToken(accountNumber));
        httpPost.addHeader("Content-type", "application/json");

        // Build JSON query:
        JsonObject json = new JsonObject();
        json.add("sharedAlbumOptions", new JsonObject()); // we keep default options: isCollaborative and isCommentable are false

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
                    throw new AuthorizationException("Could not parse album sharing response as String: " + response.getEntity());
                }

                ShareResult shareResult = new Gson().fromJson(responseText, ShareResult.class);

                // Transplant the shareinfo to the given album
                album.setShareInfo(shareResult.getShareInfo());

                // and return it.
            }
            else {
                throw new CommunicationException("Server returned an error when sharing album: " + getResponseError(response));
            }
        }
        catch (IOException e) {
            throw new CommunicationException("Error sharing album", e);
        }
    }

    /**
     * Retrieve a specific album.
     * Implements https://developers.google.com/photos/library/reference/rest/v1/albums/get
     * @param client the {@link CloseableHttpClient}
     * @param accountNumber the number of this account among Google Photos accounts
     * @param albumId the ID of the album to retrieve
     * @return the retrieved album
     * @throws AuthorizationException if user has no, or insufficient, authorizations, or if a token error occurs
     * @throws CommunicationException if an url, network or decoding error occurs
     */
    @SuppressWarnings("unused")
    private Album getAlbumById(CloseableHttpClient client, String accountNumber, String albumId) throws AuthorizationException, CommunicationException {
        HttpGet httpGet = new HttpGet("https://photoslibrary.googleapis.com/v1/albums/" + albumId);

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
                return new Gson().fromJson(responseText, Album.class);
            }
            else {
                throw new CommunicationException("Server returned an error when listing albums: " + getResponseError(response));
            }
        }
        catch (IOException e) {
            throw new CommunicationException(e);
        }
    }

    /**
     * Create an application album with the given name.
     * Implements https://developers.google.com/photos/library/reference/rest/v1/albums/create
     * @param client the {@link CloseableHttpClient}
     * @param accountNumber the number of this account among Google Photos accounts
     * @param albumName the name of the album to create
     * @return the created album
     * @throws AuthorizationException if user has no, or insufficient, authorizations, or if a token error occurs
     * @throws CommunicationException if an url, network or decoding error occurs
     */
    private Album createAlbum(CloseableHttpClient client, String accountNumber, String albumName) throws AuthorizationException, CommunicationException {
        HttpPost httpPost = new HttpPost("https://photoslibrary.googleapis.com/v1/albums");

        httpPost.addHeader("Authorization", "Bearer " + getAccessToken(accountNumber));
        httpPost.addHeader("Content-type", "application/json");

        Album album = new Album();
        album.setTitle(albumName);

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
                return gson.fromJson(responseText, Album.class);
            }
            else {
                throw new CommunicationException("Server returned an error when creating album: " + getResponseError(response));
            }
        }
        catch (IOException e) {
            throw new CommunicationException(e);
        }
    }

//    /**
//     * Upload the captured file contents to Google Photos.
//     * Implements https://developers.google.com/photos/library/guides/upload-media#uploading-bytes
//     *
//     * @param client the {@link CloseableHttpClient}
//     * @param accountNumber the number of this account among Google Photos accounts
//     * @param file to upload
//     * @return the uploadToken to be used to link this content to a media
//     * @throws AuthorizationException if user has no, or insufficient, authorizations, or if a token error occurs
//     * @throws CommunicationException if an url, network or decoding error occurs
//     * @throws UploadException if an upload-specfic error occurs
//     */
//    private String uploadFileBytesSimple(CloseableHttpClient client, String accountNumber, File file) throws AuthorizationException, UploadException, CommunicationException {
//        String uploadToken;
//
//        HttpPost httpPost = new HttpPost("https://photoslibrary.googleapis.com/v1/uploads");
//
//        httpPost.addHeader("Authorization", "Bearer " + getAccessToken(accountNumber));
//        httpPost.addHeader("Content-type", "application/octet-stream");
//        httpPost.addHeader("X-Goog-Upload-Content-Type", "mime-type");
//        httpPost.addHeader("X-Goog-Upload-Protocol", "raw");
//
//        httpPost.setEntity(new FileEntity(file, ContentType.APPLICATION_OCTET_STREAM));
//        try {
//            CloseableHttpResponse response = client.execute(httpPost);
//            if (isStatusOK(response.getCode())) {
//                try {
//                    uploadToken = EntityUtils.toString(response.getEntity());
//                }
//                catch (ParseException e) {
//                    throw new CommunicationException("Could not parse media upload response as String: " + response.getEntity());
//                }
//            }
//            else {
//                throw new UploadException("Server returned an error when uploading file contents: " + getResponseError(response));
//            }
//        }
//        catch (IOException e) {
//            throw new CommunicationException("Error uploading file contents", e);
//        }
//        return uploadToken;
//    }

    /**
     * Upload the captured file contents to Google Photos in "resumable" mode
     * Implements https://developers.google.com/photos/library/guides/resumable-uploads
     *
     * @param client the {@link CloseableHttpClient}
     * @param accountNumber the number of this account among Google Photos accounts
     * @param capture to upload
     * @return the uploadToken to be used to link this content to a media
     * @throws AuthorizationException if user has no, or insufficient, authorizations, or if a token error occurs
     * @throws CommunicationException if an url, network or decoding error occurs
     * @throws UploadException if an upload-specfic error occurs
     */
    private String uploadFileBytes(CloseableHttpClient client, String accountNumber, Capture capture) throws AuthorizationException, UploadException, CommunicationException {
        String uploadToken = null;

        final File file = capture.transientGetFile();

        // Step 1: Initiating an upload session
        logProgress("Uploading", 10);
        HttpPost httpPost = new HttpPost("https://photoslibrary.googleapis.com/v1/uploads");

        httpPost.addHeader("Authorization", "Bearer " + getAccessToken(accountNumber));
        //httpPost.addHeader("Content-Length", 0); // Don't put it here, it causes a "dupe header" error if there is an entity, and if there is no entity it's forbidden.
        httpPost.addHeader("X-Goog-Upload-Command", "start");
        httpPost.addHeader("X-Goog-Upload-Content-Type", capture.isVideo() ? "video/mp4" : "image/png");
        httpPost.addHeader("X-Goog-Upload-File-Name", capture.getName());
        httpPost.addHeader("X-Goog-Upload-Protocol", "resumable");
        httpPost.addHeader("X-Goog-Upload-Raw-Size", file.length());

        String uploadUrl;
        int chunkGranularityBytes;

        httpPost.setEntity(EMPTY_ENTITY);

        try {
            CloseableHttpResponse response = client.execute(httpPost);
            if (isStatusOK(response.getCode())) {
                try {
                    final Header uploadUrlHeader = response.getHeader("X-Goog-Upload-URL");
                    final Header chunkGranularityHeader = response.getHeader("X-Goog-Upload-Chunk-Granularity");
                    if (uploadUrlHeader != null && chunkGranularityHeader != null) {
                        // Step 2: "Saving" the session URL
                        uploadUrl = uploadUrlHeader.getValue();
                        try {
                            chunkGranularityBytes = Integer.parseInt(chunkGranularityHeader.getValue());
                        }
                        catch (NumberFormatException e) {
                            throw new CommunicationException("Could not parse X-Goog-Upload-Chunk-Granularity=" + chunkGranularityHeader.getValue() + " as integer");
                        }
                    }
                    else {
                        throw new CommunicationException("Server did not return the expected headers: X-Goog-Upload-URL=" + uploadUrlHeader + " and X-Goog-Upload-Chunk-Granularity=" + chunkGranularityHeader);
                    }
                }
                catch (ProtocolException e) {
                    throw new CommunicationException("Protocol exception initializing upload: " + response.getEntity());
                }
            }
            else {
                throw new UploadException("Server returned an error when uploading file contents: " + getResponseError(response));
            }
        }
        catch (IOException e) {
            throw new CommunicationException("Error uploading file contents", e);
        }

        // Step 3: Uploading the file

        int maxChunkSize = CHUNK_SIZE;
        maxChunkSize = (maxChunkSize/chunkGranularityBytes) * chunkGranularityBytes;
        byte[] buffer = new byte[maxChunkSize];
        int offset = 0;
        long remainingBytes = file.length();
        InputStream is;
        try {
            is = new FileInputStream(file);
        }
        catch (FileNotFoundException e) {
            throw new UploadException("File not found: " + file.getAbsolutePath());
        }
        while (remainingBytes > 0) {
            String command = (remainingBytes > maxChunkSize) ? "upload" : "upload, finalize";
            int chunkSize = (int) Math.min(maxChunkSize, remainingBytes);
            final int bytesRead;
            try {
                bytesRead = is.read(buffer, 0, chunkSize);
            }
            catch (IOException e) {
                throw new UploadException("Could not read bytes from file");
            }

            logProgress("Uploading", (int) (10 + (80 * offset)/file.length()), offset, file.length());

            httpPost = new HttpPost(uploadUrl);
            httpPost.addHeader("Authorization", "Bearer " + getAccessToken(accountNumber));
            //httpPost.addHeader("Content-Length", chunkSize); // Don't put it here, it causes a "dupe header" error as there is an entity.
            httpPost.addHeader("X-Goog-Upload-Command", command);
            httpPost.addHeader("X-Goog-Upload-Offset", offset);

            httpPost.setEntity(new ByteArrayEntity(buffer, 0, bytesRead, ContentType.APPLICATION_OCTET_STREAM));

            try {
                CloseableHttpResponse response = client.execute(httpPost);
                if (isStatusOK(response.getCode())) {
                    try {
                        uploadToken = EntityUtils.toString(response.getEntity());
                    }
                    catch (ParseException e) {
                        throw new CommunicationException("Could not parse media upload response as String: " + response.getEntity());
                    }
                }
                else {
                    final String responseError = getResponseError(response);
                    if ((response.getCode() / 100) == 5) {
                        // Error 5xx
                        throw new UploadException("Resuming not implemented yet: " + responseError);
                    }
                    throw new UploadException("Server returned an error when uploading file contents: " + responseError);
                }
            }
            catch (IOException e) {
                throw new CommunicationException("Error uploading file contents", e);
            }


            offset += bytesRead;
            remainingBytes = file.length() - offset;
        }
        logProgress("Uploaded", 90, file.length(), file.length());

        return uploadToken;
    }

    /**
     * Creates a media entry in the given application album.
     * Implements https://developers.google.com/photos/library/reference/rest/v1/mediaItems/batchCreate
     * @param client the {@link CloseableHttpClient}
     * @param accountNumber the number of this account among Google Photos accounts
     * @param capture The object representing the captured screenshot or video
     * @param albumId The ID of the album to create the capture in
     * @param uploadToken The token to the actual contents uploaded using uploadFileBytes
     * @return the ID of the created media
     * @throws AuthorizationException if user has no, or insufficient, authorizations, or if a token error occurs
     * @throws CommunicationException if an url, network or decoding error occurs
     * @throws UploadException if an upload-specfic error occurs
     */
    private String createMediaItem(CloseableHttpClient client, String accountNumber, Capture capture, String albumId, String uploadToken) throws AuthorizationException, UploadException, CommunicationException {

        logProgress("Creating media", 95);
        HttpPost httpPost = new HttpPost("https://photoslibrary.googleapis.com/v1/mediaItems:batchCreate");

        httpPost.addHeader("Authorization", "Bearer " + getAccessToken(accountNumber));
        httpPost.addHeader("Content-type", "application/json");

        // Build JSON query:
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

                // Note: this is the private link to the picture (only visible by the Google account owner) :
                // mediaItemResult.getMediaItem().getProductUrl();

                return mediaItemResult.getMediaItem().getId();
            }
            else {
                throw new UploadException("Server returned an error when creating media: " + getResponseError(response));
            }
        }
        catch (IOException e) {
            throw new CommunicationException("Error creating media", e);
        }
    }



    ////////////////////////////////////////////////////
    // Autogenerated pojos for (non-Map) Json parsing
    // Created by http://jsonschema2pojo.org
    ////////////////////////////////////////////////////


    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
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


    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    public static class Photo {
        public Photo() {
        }
    }

    @SuppressWarnings("unused")
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


    /**
     * AlbumList example
     * {
     *   "albums": [
     *     {
     *       "id": "ABWT9pnV2YqWyhiUj4oTewsezef9BLHEyRIdNyXx6rkvwjc0gMzsl6la2CY5R_YD9JllYuOBP_OJ",
     *       "title": "Ginj",
     *       "productUrl": "https://photos.google.com/lr/album/ABWT9pnV2YqWyhiUj4oTewsezef9BLHEyRIdNyXx6rkvwjc0gMzsl6la2CY5R_YD9JllYuOBP_OJ",
     *       "isWriteable": true,
     *       "shareInfo": {
     *         "sharedAlbumOptions": {},
     *         "shareableUrl": "https://photos.app.goo.gl/4uskYMvDvdSfHPTD7",
     *         "shareToken": "AOVP0rSljkRvVA9-0d0R4GsAp3WlH9zLEr3x8BoAoHdBMLA8qOe2UQnvS3PSL4_SjtpDu8dtebMlAxteF1AADUx6vrP9Xnr6vztdhHynJqFrY-QzXAwDXpTu9kWVmhkBLTg",
     *         "isJoined": true,
     *         "isOwned": true
     *       },
     *       "mediaItemsCount": "8",
     *       "coverPhotoBaseUrl": "https://lh3.googleusercontent.com/lr/AFBm1_Zo4K2OVb-vCI1LEty3Ec7l8HemK_gJfjT1bSaISMnq8_pRe-jm7m92d6WBovPX59M7mYHQ1NSpaF842AOBYiJvT6XrIJaksi1TZNCZdcIq2MiU3AB3VZiGmVla9Vjk-HA-C2VQjHQhiIXobNZbaPcyyRSJOGKxAlSQ7lzXvglD_5VajflbWNpGATLNtZaE-JjwWRqL8pLUKsY3FfUFcqVe1olT143Tpfz6HL85mp1vqWCCQYgNkjkdyTT2vnMtMECQpRJMvGyEDPOzZn7TWCR4aRm_HrpNSy8spGAlkzUK7eVEnQmTAQq1bJEvD3jT8kLBJVCGu5yBe6BsnVWFAu4mXySS2ZJA6cohfn7p3S_gLNqCj2wPYPJOsjdUtID7IGYqssuhwNxA54xv__JZysJ6g9a8bwlD340JuZo0aZqqzP1GzJ7nC4YkauG7b33U9GzAOMy5Ed7R8XZElrIi6yyW1NX9VpQbslbDhrNdQ8LrWzkMZLD7hl4Gd5SL_-pjcsIv-BDu64XYqd_PLlUPhJ_hsFjZZQlxsbanlad0tSZuDhJZwnYzVwDm2lJPOPsTFZUTyaLO1etSd-RTbUnCVzXzSMz8Og-x6C426m5L1vYWG6ub1RVR_Yt2bBximegIUPwv4MApxRqzHV57zB2Wjnsy6_Zw3EtLtdLRaCdo8RYkO0qL09TtvgHHkgFb4pMEYp4oE5m9ay7AKtt6W2hzcNKUq5ziN3n46xK8a8qAzzdiyXvBasUSCy_MfBeYzq20oOGhiqTN9ccS_af3-pIbIHvMWwwNO4s1V-vmuLbQ-CYqRHmRZv3dDm_cFAxGWAs6aw",
     *       "coverPhotoMediaItemId": "ABWT9pl3-DjE3dCouIYTnOSeTUUtDH3chFO0lU82ZdsIA0FwUpyTyJT0jUqckemBl4RoHZrLQMEsYfeBa4A9S0YJJVFbFdw0LQ"
     *     }
     *   ]
     * }
     */
    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
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
        @SerializedName("isWriteable")
        @Expose
        private Boolean isWriteable;
        @SerializedName("shareInfo")
        @Expose
        private ShareInfo shareInfo;
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
         *
         */
        public Album() {
        }

        public Album(String id, String title, String productUrl, Boolean isWriteable, ShareInfo shareInfo, String mediaItemsCount, String coverPhotoBaseUrl, String coverPhotoMediaItemId) {
            super();
            this.id = id;
            this.title = title;
            this.productUrl = productUrl;
            this.isWriteable = isWriteable;
            this.shareInfo = shareInfo;
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

        public Boolean getIsWriteable() {
            return isWriteable;
        }

        public void setIsWriteable(Boolean isWriteable) {
            this.isWriteable = isWriteable;
        }

        public ShareInfo getShareInfo() {
            return shareInfo;
        }

        public void setShareInfo(ShareInfo shareInfo) {
            this.shareInfo = shareInfo;
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

    /**
     * ShareResult example:
     *
     * {
     *     "shareInfo":
     *     {
     *         "sharedAlbumOptions": {
     *         "isCollaborative": false,
     *                 "isCommentable": false
     *     },
     *         "shareableUrl": "http://fqsdfsd.com",
     *             "shareToken": "12345sqsd",
     *             "isJoined": false,
     *             "isOwned": true
     *     }
     * }
     */
    @SuppressWarnings("unused")
    public static class ShareResult {

        @SerializedName("shareInfo")
        @Expose
        private ShareInfo shareInfo;

        /**
         * No args constructor for use in serialization
         */
        public ShareResult() {
        }

        public ShareResult(ShareInfo shareInfo) {
            super();
            this.shareInfo = shareInfo;
        }

        public ShareInfo getShareInfo() {
            return shareInfo;
        }

        public void setShareInfo(ShareInfo shareInfo) {
            this.shareInfo = shareInfo;
        }

    }


    @SuppressWarnings("unused")
    public static class ShareInfo {

        @SerializedName("sharedAlbumOptions")
        @Expose
        private SharedAlbumOptions sharedAlbumOptions;
        @SerializedName("shareableUrl")
        @Expose
        private String shareableUrl;
        @SerializedName("shareToken")
        @Expose
        private String shareToken;
        @SerializedName("isJoined")
        @Expose
        private Boolean isJoined;
        @SerializedName("isOwned")
        @Expose
        private Boolean isOwned;

        /**
         * No args constructor for use in serialization
         */
        public ShareInfo() {
        }

        public ShareInfo(SharedAlbumOptions sharedAlbumOptions, String shareableUrl, String shareToken, Boolean isJoined, Boolean isOwned) {
            super();
            this.sharedAlbumOptions = sharedAlbumOptions;
            this.shareableUrl = shareableUrl;
            this.shareToken = shareToken;
            this.isJoined = isJoined;
            this.isOwned = isOwned;
        }

        public SharedAlbumOptions getSharedAlbumOptions() {
            return sharedAlbumOptions;
        }

        public void setSharedAlbumOptions(SharedAlbumOptions sharedAlbumOptions) {
            this.sharedAlbumOptions = sharedAlbumOptions;
        }

        public String getShareableUrl() {
            return shareableUrl;
        }

        public void setShareableUrl(String shareableUrl) {
            this.shareableUrl = shareableUrl;
        }

        public String getShareToken() {
            return shareToken;
        }

        public void setShareToken(String shareToken) {
            this.shareToken = shareToken;
        }

        public Boolean getIsJoined() {
            return isJoined;
        }

        public void setIsJoined(Boolean isJoined) {
            this.isJoined = isJoined;
        }

        public Boolean getIsOwned() {
            return isOwned;
        }

        public void setIsOwned(Boolean isOwned) {
            this.isOwned = isOwned;
        }

    }


    @SuppressWarnings("unused")
    public static class SharedAlbumOptions {

        @SerializedName("isCollaborative")
        @Expose
        private Boolean isCollaborative;
        @SerializedName("isCommentable")
        @Expose
        private Boolean isCommentable;

        /**
         * No args constructor for use in serialization
         */
        public SharedAlbumOptions() {
        }

        public SharedAlbumOptions(Boolean isCollaborative, Boolean isCommentable) {
            super();
            this.isCollaborative = isCollaborative;
            this.isCommentable = isCommentable;
        }

        public Boolean getIsCollaborative() {
            return isCollaborative;
        }

        public void setIsCollaborative(Boolean isCollaborative) {
            this.isCollaborative = isCollaborative;
        }

        public Boolean getIsCommentable() {
            return isCommentable;
        }

        public void setIsCommentable(Boolean isCommentable) {
            this.isCommentable = isCommentable;
        }

    }


}
