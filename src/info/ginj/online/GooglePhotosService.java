package info.ginj.online;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import info.ginj.Capture;
import info.ginj.Ginj;
import info.ginj.Prefs;
import info.ginj.online.exception.AuthorizationException;
import info.ginj.online.exception.CommunicationException;
import info.ginj.online.exception.UploadException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.FileEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles interaction with Google Photos service
 * See also
 * https://developers.google.com/photos/library/guides/authorization
 * <p>
 * TODO: when creating account, remember to tell user that Ginj medias are uploaded in full quality and will count in the user quota
 * TODO: videos must be max 10GB
 */
public class GooglePhotosService extends GoogleService implements OnlineService {

    // "Access to create an album, share it, upload media items to it, and join a shared album."
    private static final String[] GOOGLE_PHOTOS_REQUIRED_SCOPES = {"https://www.googleapis.com/auth/photoslibrary.appendonly", "https://www.googleapis.com/auth/photoslibrary.readonly.appcreateddata", "https://www.googleapis.com/auth/photoslibrary.sharing"};

    @Override
    public String getServiceName() {
        return "Google Photos";
    }

    @Override
    public String[] getRequiredScopes() {
        return GooglePhotosService.GOOGLE_PHOTOS_REQUIRED_SCOPES;
    }

    protected Prefs.Key getRefreshTokenKeyPrefix() {
        return Prefs.Key.EXPORTER_GOOGLE_PHOTOS_REFRESH_TOKEN_PREFIX;
    }

    protected Prefs.Key getAccessTokenKeyPrefix() {
        return Prefs.Key.EXPORTER_GOOGLE_PHOTOS_ACCESS_TOKEN_PREFIX;
    }

    protected Prefs.Key getAccessExpiryKeyPrefix() {
        return Prefs.Key.EXPORTER_GOOGLE_PHOTOS_ACCESS_EXPIRY_PREFIX;
    }


    @Override
    public String uploadCapture(Capture capture, String accountNumber) throws AuthorizationException, UploadException, CommunicationException {
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
        return shareMedia(mediaId, accountNumber);
    }


    /**
     * Implements https://developers.google.com/photos/library/reference/rest/v1/albums/share
     */
    private String shareMedia(String id, String accountNumber) throws AuthorizationException, CommunicationException {
        CloseableHttpClient client = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost("https://photoslibrary.googleapis.com/v1/albums/" + id + ":share");

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
                    throw new AuthorizationException("Could not parse media sharing response as String: " + response.getEntity());
                }

                ShareResult shareResult = new Gson().fromJson(responseText, ShareResult.class);

                return shareResult.getShareInfo().getShareableUrl();
            }
            else {
                throw new CommunicationException("Server returned code " + getResponseError(response) + " when sharing media");
            }
        }
        catch (IOException e) {
            throw new CommunicationException("Error sharing media", e);
        }
    }

    private String createMediaItem(Capture capture, String accountNumber, String albumId, String uploadToken) throws AuthorizationException, UploadException {
        CloseableHttpClient client = HttpClients.createDefault();

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


    ////////////////////////////////////////////////////
    // Autogenerated pojos for complex Json parsing
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


    ////////////////////
    // Album-related

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


    ///////////////////////
    //  Sharing result
    //  Example:
    //
    //    {
    //        "shareInfo":
    //        {
    //            "sharedAlbumOptions": {
    //              "isCollaborative": false,
    //              "isCommentable": false
    //            },
    //            "shareableUrl": "http://fqsdfsd.com",
    //            "shareToken": "12345sqsd",
    //            "isJoined": false,
    //            "isOwned": true
    //        }
    //    }


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
