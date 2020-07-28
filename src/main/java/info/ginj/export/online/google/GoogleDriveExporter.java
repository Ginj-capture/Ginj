package info.ginj.export.online.google;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import info.ginj.export.online.OnlineExporter;
import info.ginj.export.online.exception.AuthorizationException;
import info.ginj.export.online.exception.CommunicationException;
import info.ginj.export.online.exception.UploadException;
import info.ginj.model.Capture;
import info.ginj.model.Export;
import info.ginj.model.Target;
import info.ginj.util.UI;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * See https://developers.google.com/drive/api/v3/manage-uploads#resumable
 * See https://developers.google.com/drive/api/v3/manage-sharing
 */
public class GoogleDriveExporter extends GoogleExporter implements OnlineExporter {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveExporter.class);

    private static final String[] GOOGLE_DRIVE_REQUIRED_SCOPES = {"https://www.googleapis.com/auth/drive"};
    public static final String NAME = "Google Drive";


    @Override
    public String getExporterName() {
        return NAME;
    }

    @Override
    protected List<String> getRequiredScopes() {
        List<String> scopes = new ArrayList<>(super.getRequiredScopes());
        scopes.addAll(Arrays.asList(GOOGLE_DRIVE_REQUIRED_SCOPES));
        return scopes;
    }

    @Override
    public String getDefaultShareText() {
        return "Add to Google Drive";
    }

    @Override
    public String getIconPath() {
        return "/img/logo/googledrive.png";
    }

    @Override
    public boolean isImageSupported() {
        return true;
    }

    @Override
    public boolean isVideoSupported() {
        return true;
    }

    /**
     * TODO pull this implementation up ! (to OnlineExporter ?)
     * <p>
     * Uploads the given capture to Google Drive.
     * This method is run in its own thread and should not access the GUI directly. All interaction
     * should go through synchronized objects or be enclosed in a SwingUtilities.invokeLater() logic
     *
     * @param capture the capture to export
     * @param target  the target to export this capture to
     */
    @Override
    public void exportCapture(Capture capture, Target target) {
        try {
            final Export export = uploadCapture(capture, target);
            String message = "Upload successful.";

            if (export.getLocation() != null) {
                if (target.getSettings().getMustCopyPath()) {
                    copyTextToClipboard(export.getLocation());
                    export.setLocationCopied(true);
                    message += "\nA link to your capture was copied to the clipboard";
                }
            }
            capture.addExport(export);
            // Indicate export is complete.
            complete(message);
        }
        catch (Exception e) {
            UI.alertException(parentFrame, getExporterName() + " Error", "There was an error exporting to " + getExporterName(), e, logger);
            failed("Upload error");
        }
    }

    /**
     * Uploads a capture to Google Drive, and optionally shares it and returns the URL of the shared media.
     *
     * @param capture The object representing the captured screenshot or video
     * @param target  the target to export this capture to
     * @return a public URL to share to give access to the uploaded media.
     * @throws AuthorizationException if user has no, or insufficient, authorizations, or if a token error occurs
     * @throws CommunicationException if an url, network or decoding error occurs
     * @throws UploadException        if an upload-specfic error occurs
     */
    @Override
    public Export uploadCapture(Capture capture, Target target) throws AuthorizationException, UploadException, CommunicationException {
        // We need an actual file (for now at least). Make sure we have or create one
        logProgress("Rendering file", PROGRESS_RENDER_START);
        try {
            capture.toRenderedFile();
        }
        catch (IOException e) {
            throw new UploadException("Error preparing file to upload", e);
        }

        final CloseableHttpClient client = HttpClients.createDefault();

        // Step 1: Upload the file

        FilesResource resource = uploadFile(client, target, capture);

        if (target.getSettings().getMustShare()) {
            // Step 2: Share it
            @SuppressWarnings("unused")
            PermissionsResource permissionsResource = shareFile(client, target, resource.getId());

            // Step 3: Refetch now that it's shared
            resource = getFilesResource(client, target, resource.getId());

            return new Export(getExporterName(), resource.getId(), resource.getWebViewLink() /*resource.getWebContentLink() is a download link */, false);
        }
        else {
            return new Export(getExporterName(), resource.getId(), null, false);
        }
    }

    /**
     * This method implements https://developers.google.com/drive/api/v3/manage-uploads?authuser=1#resumable
     *
     * @param client  the {@link CloseableHttpClient}
     * @param target  the target to export this capture to
     * @param capture the object representing the captured screenshot or video
     * @return a public URL to share to give access to the uploaded media.
     * @throws AuthorizationException if user has no, or insufficient, authorizations, or if a token error occurs
     * @throws CommunicationException if an url, network or decoding error occurs
     * @throws UploadException        if an upload-specfic error occurs
     */
    private FilesResource uploadFile(CloseableHttpClient client, Target target, Capture capture) throws AuthorizationException, CommunicationException, UploadException {
        final File file = capture.getRenderedFile();

        // Step 1: Initiating an upload session
        logProgress("Uploading", PROGRESS_UPLOAD_START);
        HttpPost httpPost = new HttpPost("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable");

        httpPost.addHeader("Authorization", "Bearer " + getAccessToken(target.getAccount()));
        httpPost.addHeader("X-Upload-Content-Type", capture.isVideo() ? "video/mp4" : "image/png");
        httpPost.addHeader("X-Upload-Content-Length", file.length()); // Not mandatory


        // Add file metadata in JSON as body. Something like:
        httpPost.addHeader("Content-Type", "application/json; charset=UTF-8");
        httpPost.setEntity(new StringEntity(
                "{\"name\": \"" + file.getName() + "\"}"
        ));


        String uploadUrl;
        try {
            CloseableHttpResponse response = client.execute(httpPost);
            if (isStatusOK(response.getCode())) {
                try {
                    final Header locationHeader = response.getHeader("Location");
                    if (locationHeader != null) {
                        // Step 2: "Saving" the session URL
                        uploadUrl = locationHeader.getValue();
                    }
                    else {
                        throw new CommunicationException("Server did not return the expected 'Location' header");
                    }
                }
                catch (ProtocolException e) {
                    throw new CommunicationException("Protocol exception initializing upload:\n" + response.getEntity());
                }
            }
            else {
                throw new UploadException("The server returned the following error when uploading file contents:\n" + getResponseError(response));
            }
        }
        catch (IOException e) {
            throw new CommunicationException("Error uploading file contents", e);
        }

        // Step 3: Uploading the file

        FilesResource fileResource = null; // Will be completed by the upload response

        int maxChunkSize = CHUNK_SIZE;
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

            int chunkSize = (int) Math.min(maxChunkSize, remainingBytes);
            final int bytesRead;
            try {
                bytesRead = is.read(buffer, 0, chunkSize);
            }
            catch (IOException e) {
                throw new UploadException("Could not read bytes from file");
            }

            logProgress("Uploading", (int) (PROGRESS_UPLOAD_START + ((PROGRESS_UPLOAD_END - PROGRESS_UPLOAD_START) * offset) / file.length()), offset, file.length());

            HttpPut httpPut = new HttpPut(uploadUrl);
            httpPut.addHeader("Authorization", "Bearer " + getAccessToken(target.getAccount()));
            //httpPost.addHeader("Content-Length", chunkSize); // Don't put it here, it causes a "dupe header" error as there is an entity.
            httpPut.addHeader("Content-Range", "bytes " + offset + "-" + (Math.min(offset + CHUNK_SIZE, file.length()) - 1) + "/" + file.length());

            httpPut.setEntity(new ByteArrayEntity(buffer, 0, bytesRead, ContentType.APPLICATION_OCTET_STREAM));

            try {
                CloseableHttpResponse response = client.execute(httpPut);
                if (offset + CHUNK_SIZE < file.length() && response.getCode() == 308) {
                    // All chunks except the last should get a 308 Resume Incomplete - This is normal
                    try {
                        String responseText = EntityUtils.toString(response.getEntity());
                        logger.debug("Response: " + responseText);
                    }
                    catch (ParseException e) {
                        throw new CommunicationException("Could not parse media upload response as String:\n" + response.getEntity());
                    }
                }
                else if (offset + CHUNK_SIZE >= file.length() && isStatusOK(response.getCode())) {
                    // Last chunk should get a 200 OK
                    try {
                        String responseText = EntityUtils.toString(response.getEntity());
                        logger.info("Response: " + responseText);
                        if (offset + CHUNK_SIZE >= file.length()) {
                            fileResource = new Gson().fromJson(responseText, FilesResource.class);
                        }
                    }
                    catch (ParseException e) {
                        throw new CommunicationException("Could not parse media upload response as String:\n" + response.getEntity());
                    }
                }
                else {
                    // All the rest is unexpected
                    final String responseError = getResponseError(response);
                    if ((response.getCode() / 100) == 5) {
                        // Error 5xx
                        throw new UploadException("Resuming not implemented yet:\n" + responseError);
                    }
                    throw new UploadException("The server returned the following error when uploading file contents:\n" + responseError);
                }
            }
            catch (IOException e) {
                throw new CommunicationException("Error uploading file contents", e);
            }

            offset += bytesRead;
            remainingBytes = file.length() - offset;
        }
        logProgress("Uploaded", PROGRESS_UPLOAD_END, file.length(), file.length());

        return fileResource;
    }

    /**
     * This method implements https://developers.google.com/drive/api/v3/reference/permissions/create
     * see https://stackoverflow.com/a/11669565/13551878
     */
    public PermissionsResource shareFile(CloseableHttpClient client, Target target, String fileId) throws AuthorizationException, CommunicationException {
        HttpPost httpPost = new HttpPost("https://www.googleapis.com/drive/v3/files/" + fileId + "/permissions");

        httpPost.addHeader("Authorization", "Bearer " + getAccessToken(target.getAccount()));
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.setEntity(new StringEntity(
                "{\"role\": \"reader\"," +
                        "\"type\": \"anyone\"}"
        ));

        // Send request
        try {
            CloseableHttpResponse response = client.execute(httpPost);
            if (isStatusOK(response.getCode())) {
                try {
                    String responseText = EntityUtils.toString(response.getEntity());
                    return new Gson().fromJson(responseText, PermissionsResource.class);
                }
                catch (ParseException e) {
                    throw new CommunicationException("Could not parse shared link creation response as String: " + response.getEntity());
                }
            }
            else {
                throw new CommunicationException("The server returned the following error when creating shared link:\n" + getResponseError(response));
            }
        }
        catch (IOException e) {
            throw new CommunicationException("Error creating shared link", e);
        }
    }


    /**
     * This method implements https://developers.google.com/drive/api/v3/reference/files/get
     */
    public FilesResource getFilesResource(CloseableHttpClient client, Target target, String fileId) throws AuthorizationException, CommunicationException {
        try {
            URIBuilder builder = new URIBuilder("https://www.googleapis.com/drive/v3/files/" + fileId);
            builder.setParameter("fields", "name, id, webViewLink, webContentLink");
            HttpGet httpGet = new HttpGet(builder.build());
            httpGet.addHeader("Authorization", "Bearer " + getAccessToken(target.getAccount()));
            httpGet.setEntity(EMPTY_ENTITY);

            // Send request
            CloseableHttpResponse response = client.execute(httpGet);
            if (isStatusOK(response.getCode())) {
                try {
                    String responseText = EntityUtils.toString(response.getEntity());
                    logger.info("Response: " + responseText);
                    return new Gson().fromJson(responseText, FilesResource.class);
                }
                catch (ParseException e) {
                    throw new CommunicationException("Could not parse media upload response as String:\n" + response.getEntity());
                }
            }
            else {
                throw new CommunicationException("The server returned the following error when creating shared link:\n" + getResponseError(response));
            }
        }
        catch (IOException | URISyntaxException e) {
            throw new CommunicationException("Error creating shared link", e);
        }
    }



    ////////////////////////////////////////////////////
    // Autogenerated pojos for (non-Map) Json parsing
    // Created by http://jsonschema2pojo.org
    ////////////////////////////////////////////////////


    //    FilesResource example :
    //
    // {
    //  "kind": "drive#file",
    //  "id": "hello",
    //  "name": "hello",
    //  "mimeType": "hello",
    //  "description": "hello",
    //  "starred": false,
    //  "trashed": false,
    //  "explicitlyTrashed": false,
    //  "trashingUser": {
    //    "kind": "drive#user",
    //    "displayName": "hello",
    //    "photoLink": "hello",
    //    "me": false,
    //    "permissionId": "hello",
    //    "emailAddress": "hello"
    //  },
    //  "trashedTime": "2014-01-01T23:28:56.782Z",
    //  "parents": [
    //    "hello"
    //  ],
    //  "properties": {
    //    "(key)": "hello"
    //  },
    //  "appProperties": {
    //    "(key)": "hello"
    //  },
    //  "spaces": [
    //    "hello"
    //  ],
    //  "version": 1234567890123,
    //  "webContentLink": "hello",
    //  "webViewLink": "hello",
    //  "iconLink": "hello",
    //  "hasThumbnail": false,
    //  "thumbnailLink": "hello",
    //  "thumbnailVersion": 1234567890123,
    //  "viewedByMe": false,
    //  "viewedByMeTime": "2014-01-01T23:28:56.782Z",
    //  "createdTime": "2014-01-01T23:28:56.782Z",
    //  "modifiedTime": "2014-01-01T23:28:56.782Z",
    //  "modifiedByMeTime": "2014-01-01T23:28:56.782Z",
    //  "modifiedByMe": false,
    //  "sharedWithMeTime": "2014-01-01T23:28:56.782Z",
    //  "sharingUser": {
    //    "kind": "drive#user",
    //    "displayName": "hello",
    //    "photoLink": "hello",
    //    "me": false,
    //    "permissionId": "hello",
    //    "emailAddress": "hello"
    //  },
    //  "owners": [
    //    {
    //      "kind": "drive#user",
    //      "displayName": "hello",
    //      "photoLink": "hello",
    //      "me": false,
    //      "permissionId": "hello",
    //      "emailAddress": "hello"
    //    }
    //  ],
    //  "teamDriveId": "hello",
    //  "driveId": "hello",
    //  "lastModifyingUser": {
    //    "kind": "drive#user",
    //    "displayName": "hello",
    //    "photoLink": "hello",
    //    "me": false,
    //    "permissionId": "hello",
    //    "emailAddress": "hello"
    //  },
    //  "shared": false,
    //  "ownedByMe": false,
    //  "capabilities": {
    //    "canAddChildren": false,
    //    "canAddFolderFromAnotherDrive": false,
    //    "canAddMyDriveParent": false,
    //    "canChangeCopyRequiresWriterPermission": false,
    //    "canChangeViewersCanCopyContent": false,
    //    "canComment": false,
    //    "canCopy": false,
    //    "canDelete": false,
    //    "canDeleteChildren": false,
    //    "canDownload": false,
    //    "canEdit": false,
    //    "canListChildren": false,
    //    "canModifyContent": false,
    //    "canMoveChildrenOutOfTeamDrive": false,
    //    "canMoveChildrenOutOfDrive": false,
    //    "canMoveChildrenWithinTeamDrive": false,
    //    "canMoveChildrenWithinDrive": false,
    //    "canMoveItemIntoTeamDrive": false,
    //    "canMoveItemOutOfTeamDrive": false,
    //    "canMoveItemOutOfDrive": false,
    //    "canMoveItemWithinTeamDrive": false,
    //    "canMoveItemWithinDrive": false,
    //    "canMoveTeamDriveItem": false,
    //    "canReadRevisions": false,
    //    "canReadTeamDrive": false,
    //    "canReadDrive": false,
    //    "canRemoveChildren": false,
    //    "canRemoveMyDriveParent": false,
    //    "canRename": false,
    //    "canShare": false,
    //    "canTrash": false,
    //    "canTrashChildren": false,
    //    "canUntrash": false
    //  },
    //  "viewersCanCopyContent": false,
    //  "copyRequiresWriterPermission": false,
    //  "writersCanShare": false,
    //  "permissions": [
    //    "(permissions Resource)"
    //  ],
    //  "permissionIds": [
    //    "hello"
    //  ],
    //  "hasAugmentedPermissions": false,
    //  "folderColorRgb": "hello",
    //  "originalFilename": "hello",
    //  "fullFileExtension": "hello",
    //  "fileExtension": "hello",
    //  "md5Checksum": "hello",
    //  "size": 1234567890123,
    //  "quotaBytesUsed": 1234567890123,
    //  "headRevisionId": "hello",
    //  "contentHints": {
    //    "thumbnail": {
    //      "image": "(bytes)",
    //      "mimeType": "hello"
    //    },
    //    "indexableText": "hello"
    //  },
    //  "imageMediaMetadata": {
    //    "width": 12345,
    //    "height": 12345,
    //    "rotation": 12345,
    //    "location": {
    //      "latitude": 123.45,
    //      "longitude": 123.45,
    //      "altitude": 123.45
    //    },
    //    "time": "hello",
    //    "cameraMake": "hello",
    //    "cameraModel": "hello",
    //    "exposureTime": 123.45,
    //    "aperture": 123.45,
    //    "flashUsed": false,
    //    "focalLength": 123.45,
    //    "isoSpeed": 12345,
    //    "meteringMode": "hello",
    //    "sensor": "hello",
    //    "exposureMode": "hello",
    //    "colorSpace": "hello",
    //    "whiteBalance": "hello",
    //    "exposureBias": 123.45,
    //    "maxApertureValue": 123.45,
    //    "subjectDistance": 12345,
    //    "lens": "hello"
    //  },
    //  "videoMediaMetadata": {
    //    "width": 12345,
    //    "height": 12345,
    //    "durationMillis": 1234567890123
    //  },
    //  "isAppAuthorized": false,
    //  "exportLinks": {
    //    "(key)": "hello"
    //  },
    //  "shortcutDetails": {
    //    "targetId": "hello",
    //    "targetMimeType": "hello"
    //  }
    //}

    @SuppressWarnings("unused")
    public static class FilesResource {

        @SerializedName("kind")
        @Expose
        private String kind;
        @SerializedName("id")
        @Expose
        private String id;
        @SerializedName("name")
        @Expose
        private String name;
        @SerializedName("mimeType")
        @Expose
        private String mimeType;
        @SerializedName("description")
        @Expose
        private String description;
        @SerializedName("starred")
        @Expose
        private Boolean starred;
        @SerializedName("trashed")
        @Expose
        private Boolean trashed;
        @SerializedName("explicitlyTrashed")
        @Expose
        private Boolean explicitlyTrashed;
        @SerializedName("trashingUser")
        @Expose
        private TrashingUser trashingUser;
        @SerializedName("trashedTime")
        @Expose
        private Date trashedTime;
        @SerializedName("parents")
        @Expose
        private List<String> parents = null;
        @SerializedName("properties")
        @Expose
        private Properties properties;
        @SerializedName("appProperties")
        @Expose
        private AppProperties appProperties;
        @SerializedName("spaces")
        @Expose
        private List<String> spaces = null;
        @SerializedName("version")
        @Expose
        private Integer version;
        @SerializedName("webContentLink")
        @Expose
        private String webContentLink;
        @SerializedName("webViewLink")
        @Expose
        private String webViewLink;
        @SerializedName("iconLink")
        @Expose
        private String iconLink;
        @SerializedName("hasThumbnail")
        @Expose
        private Boolean hasThumbnail;
        @SerializedName("thumbnailLink")
        @Expose
        private String thumbnailLink;
        @SerializedName("thumbnailVersion")
        @Expose
        private Integer thumbnailVersion;
        @SerializedName("viewedByMe")
        @Expose
        private Boolean viewedByMe;
        @SerializedName("viewedByMeTime")
        @Expose
        private Date viewedByMeTime;
        @SerializedName("createdTime")
        @Expose
        private Date createdTime;
        @SerializedName("modifiedTime")
        @Expose
        private Date modifiedTime;
        @SerializedName("modifiedByMeTime")
        @Expose
        private Date modifiedByMeTime;
        @SerializedName("modifiedByMe")
        @Expose
        private Boolean modifiedByMe;
        @SerializedName("sharedWithMeTime")
        @Expose
        private Date sharedWithMeTime;
        @SerializedName("sharingUser")
        @Expose
        private SharingUser sharingUser;
        @SerializedName("owners")
        @Expose
        private List<Owner> owners = null;
        @SerializedName("teamDriveId")
        @Expose
        private String teamDriveId;
        @SerializedName("driveId")
        @Expose
        private String driveId;
        @SerializedName("lastModifyingUser")
        @Expose
        private LastModifyingUser lastModifyingUser;
        @SerializedName("shared")
        @Expose
        private Boolean shared;
        @SerializedName("ownedByMe")
        @Expose
        private Boolean ownedByMe;
        @SerializedName("capabilities")
        @Expose
        private Capabilities capabilities;
        @SerializedName("viewersCanCopyContent")
        @Expose
        private Boolean viewersCanCopyContent;
        @SerializedName("copyRequiresWriterPermission")
        @Expose
        private Boolean copyRequiresWriterPermission;
        @SerializedName("writersCanShare")
        @Expose
        private Boolean writersCanShare;
        @SerializedName("permissions")
        @Expose
        private List<String> permissions = null;
        @SerializedName("permissionIds")
        @Expose
        private List<String> permissionIds = null;
        @SerializedName("hasAugmentedPermissions")
        @Expose
        private Boolean hasAugmentedPermissions;
        @SerializedName("folderColorRgb")
        @Expose
        private String folderColorRgb;
        @SerializedName("originalFilename")
        @Expose
        private String originalFilename;
        @SerializedName("fullFileExtension")
        @Expose
        private String fullFileExtension;
        @SerializedName("fileExtension")
        @Expose
        private String fileExtension;
        @SerializedName("md5Checksum")
        @Expose
        private String md5Checksum;
        @SerializedName("size")
        @Expose
        private Integer size;
        @SerializedName("quotaBytesUsed")
        @Expose
        private Integer quotaBytesUsed;
        @SerializedName("headRevisionId")
        @Expose
        private String headRevisionId;
        @SerializedName("contentHints")
        @Expose
        private ContentHints contentHints;
        @SerializedName("imageMediaMetadata")
        @Expose
        private ImageMediaMetadata imageMediaMetadata;
        @SerializedName("videoMediaMetadata")
        @Expose
        private VideoMediaMetadata videoMediaMetadata;
        @SerializedName("isAppAuthorized")
        @Expose
        private Boolean isAppAuthorized;
        @SerializedName("exportLinks")
        @Expose
        private ExportLinks exportLinks;
        @SerializedName("shortcutDetails")
        @Expose
        private ShortcutDetails shortcutDetails;

        /**
         * No args constructor for use in serialization
         */
        public FilesResource() {
        }

        public FilesResource(String kind, String id, String name, String mimeType, String description, Boolean starred, Boolean trashed, Boolean explicitlyTrashed, TrashingUser trashingUser, Date trashedTime, List<String> parents, Properties properties, AppProperties appProperties, List<String> spaces, Integer version, String webContentLink, String webViewLink, String iconLink, Boolean hasThumbnail, String thumbnailLink, Integer thumbnailVersion, Boolean viewedByMe, Date viewedByMeTime, Date createdTime, Date modifiedTime, Date modifiedByMeTime, Boolean modifiedByMe, Date sharedWithMeTime, SharingUser sharingUser, List<Owner> owners, String teamDriveId, String driveId, LastModifyingUser lastModifyingUser, Boolean shared, Boolean ownedByMe, Capabilities capabilities, Boolean viewersCanCopyContent, Boolean copyRequiresWriterPermission, Boolean writersCanShare, List<String> permissions, List<String> permissionIds, Boolean hasAugmentedPermissions, String folderColorRgb, String originalFilename, String fullFileExtension, String fileExtension, String md5Checksum, Integer size, Integer quotaBytesUsed, String headRevisionId, ContentHints contentHints, ImageMediaMetadata imageMediaMetadata, VideoMediaMetadata videoMediaMetadata, Boolean isAppAuthorized, ExportLinks exportLinks, ShortcutDetails shortcutDetails) {
            super();
            this.kind = kind;
            this.id = id;
            this.name = name;
            this.mimeType = mimeType;
            this.description = description;
            this.starred = starred;
            this.trashed = trashed;
            this.explicitlyTrashed = explicitlyTrashed;
            this.trashingUser = trashingUser;
            this.trashedTime = trashedTime;
            this.parents = parents;
            this.properties = properties;
            this.appProperties = appProperties;
            this.spaces = spaces;
            this.version = version;
            this.webContentLink = webContentLink;
            this.webViewLink = webViewLink;
            this.iconLink = iconLink;
            this.hasThumbnail = hasThumbnail;
            this.thumbnailLink = thumbnailLink;
            this.thumbnailVersion = thumbnailVersion;
            this.viewedByMe = viewedByMe;
            this.viewedByMeTime = viewedByMeTime;
            this.createdTime = createdTime;
            this.modifiedTime = modifiedTime;
            this.modifiedByMeTime = modifiedByMeTime;
            this.modifiedByMe = modifiedByMe;
            this.sharedWithMeTime = sharedWithMeTime;
            this.sharingUser = sharingUser;
            this.owners = owners;
            this.teamDriveId = teamDriveId;
            this.driveId = driveId;
            this.lastModifyingUser = lastModifyingUser;
            this.shared = shared;
            this.ownedByMe = ownedByMe;
            this.capabilities = capabilities;
            this.viewersCanCopyContent = viewersCanCopyContent;
            this.copyRequiresWriterPermission = copyRequiresWriterPermission;
            this.writersCanShare = writersCanShare;
            this.permissions = permissions;
            this.permissionIds = permissionIds;
            this.hasAugmentedPermissions = hasAugmentedPermissions;
            this.folderColorRgb = folderColorRgb;
            this.originalFilename = originalFilename;
            this.fullFileExtension = fullFileExtension;
            this.fileExtension = fileExtension;
            this.md5Checksum = md5Checksum;
            this.size = size;
            this.quotaBytesUsed = quotaBytesUsed;
            this.headRevisionId = headRevisionId;
            this.contentHints = contentHints;
            this.imageMediaMetadata = imageMediaMetadata;
            this.videoMediaMetadata = videoMediaMetadata;
            this.isAppAuthorized = isAppAuthorized;
            this.exportLinks = exportLinks;
            this.shortcutDetails = shortcutDetails;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Boolean getStarred() {
            return starred;
        }

        public void setStarred(Boolean starred) {
            this.starred = starred;
        }

        public Boolean getTrashed() {
            return trashed;
        }

        public void setTrashed(Boolean trashed) {
            this.trashed = trashed;
        }

        public Boolean getExplicitlyTrashed() {
            return explicitlyTrashed;
        }

        public void setExplicitlyTrashed(Boolean explicitlyTrashed) {
            this.explicitlyTrashed = explicitlyTrashed;
        }

        public TrashingUser getTrashingUser() {
            return trashingUser;
        }

        public void setTrashingUser(TrashingUser trashingUser) {
            this.trashingUser = trashingUser;
        }

        public Date getTrashedTime() {
            return trashedTime;
        }

        public void setTrashedTime(Date trashedTime) {
            this.trashedTime = trashedTime;
        }

        public List<String> getParents() {
            return parents;
        }

        public void setParents(List<String> parents) {
            this.parents = parents;
        }

        public Properties getProperties() {
            return properties;
        }

        public void setProperties(Properties properties) {
            this.properties = properties;
        }

        public AppProperties getAppProperties() {
            return appProperties;
        }

        public void setAppProperties(AppProperties appProperties) {
            this.appProperties = appProperties;
        }

        public List<String> getSpaces() {
            return spaces;
        }

        public void setSpaces(List<String> spaces) {
            this.spaces = spaces;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }

        public String getWebContentLink() {
            return webContentLink;
        }

        public void setWebContentLink(String webContentLink) {
            this.webContentLink = webContentLink;
        }

        public String getWebViewLink() {
            return webViewLink;
        }

        public void setWebViewLink(String webViewLink) {
            this.webViewLink = webViewLink;
        }

        public String getIconLink() {
            return iconLink;
        }

        public void setIconLink(String iconLink) {
            this.iconLink = iconLink;
        }

        public Boolean getHasThumbnail() {
            return hasThumbnail;
        }

        public void setHasThumbnail(Boolean hasThumbnail) {
            this.hasThumbnail = hasThumbnail;
        }

        public String getThumbnailLink() {
            return thumbnailLink;
        }

        public void setThumbnailLink(String thumbnailLink) {
            this.thumbnailLink = thumbnailLink;
        }

        public Integer getThumbnailVersion() {
            return thumbnailVersion;
        }

        public void setThumbnailVersion(Integer thumbnailVersion) {
            this.thumbnailVersion = thumbnailVersion;
        }

        public Boolean getViewedByMe() {
            return viewedByMe;
        }

        public void setViewedByMe(Boolean viewedByMe) {
            this.viewedByMe = viewedByMe;
        }

        public Date getViewedByMeTime() {
            return viewedByMeTime;
        }

        public void setViewedByMeTime(Date viewedByMeTime) {
            this.viewedByMeTime = viewedByMeTime;
        }

        public Date getCreatedTime() {
            return createdTime;
        }

        public void setCreatedTime(Date createdTime) {
            this.createdTime = createdTime;
        }

        public Date getModifiedTime() {
            return modifiedTime;
        }

        public void setModifiedTime(Date modifiedTime) {
            this.modifiedTime = modifiedTime;
        }

        public Date getModifiedByMeTime() {
            return modifiedByMeTime;
        }

        public void setModifiedByMeTime(Date modifiedByMeTime) {
            this.modifiedByMeTime = modifiedByMeTime;
        }

        public Boolean getModifiedByMe() {
            return modifiedByMe;
        }

        public void setModifiedByMe(Boolean modifiedByMe) {
            this.modifiedByMe = modifiedByMe;
        }

        public Date getSharedWithMeTime() {
            return sharedWithMeTime;
        }

        public void setSharedWithMeTime(Date sharedWithMeTime) {
            this.sharedWithMeTime = sharedWithMeTime;
        }

        public SharingUser getSharingUser() {
            return sharingUser;
        }

        public void setSharingUser(SharingUser sharingUser) {
            this.sharingUser = sharingUser;
        }

        public List<Owner> getOwners() {
            return owners;
        }

        public void setOwners(List<Owner> owners) {
            this.owners = owners;
        }

        public String getTeamDriveId() {
            return teamDriveId;
        }

        public void setTeamDriveId(String teamDriveId) {
            this.teamDriveId = teamDriveId;
        }

        public String getDriveId() {
            return driveId;
        }

        public void setDriveId(String driveId) {
            this.driveId = driveId;
        }

        public LastModifyingUser getLastModifyingUser() {
            return lastModifyingUser;
        }

        public void setLastModifyingUser(LastModifyingUser lastModifyingUser) {
            this.lastModifyingUser = lastModifyingUser;
        }

        public Boolean getShared() {
            return shared;
        }

        public void setShared(Boolean shared) {
            this.shared = shared;
        }

        public Boolean getOwnedByMe() {
            return ownedByMe;
        }

        public void setOwnedByMe(Boolean ownedByMe) {
            this.ownedByMe = ownedByMe;
        }

        public Capabilities getCapabilities() {
            return capabilities;
        }

        public void setCapabilities(Capabilities capabilities) {
            this.capabilities = capabilities;
        }

        public Boolean getViewersCanCopyContent() {
            return viewersCanCopyContent;
        }

        public void setViewersCanCopyContent(Boolean viewersCanCopyContent) {
            this.viewersCanCopyContent = viewersCanCopyContent;
        }

        public Boolean getCopyRequiresWriterPermission() {
            return copyRequiresWriterPermission;
        }

        public void setCopyRequiresWriterPermission(Boolean copyRequiresWriterPermission) {
            this.copyRequiresWriterPermission = copyRequiresWriterPermission;
        }

        public Boolean getWritersCanShare() {
            return writersCanShare;
        }

        public void setWritersCanShare(Boolean writersCanShare) {
            this.writersCanShare = writersCanShare;
        }

        public List<String> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<String> permissions) {
            this.permissions = permissions;
        }

        public List<String> getPermissionIds() {
            return permissionIds;
        }

        public void setPermissionIds(List<String> permissionIds) {
            this.permissionIds = permissionIds;
        }

        public Boolean getHasAugmentedPermissions() {
            return hasAugmentedPermissions;
        }

        public void setHasAugmentedPermissions(Boolean hasAugmentedPermissions) {
            this.hasAugmentedPermissions = hasAugmentedPermissions;
        }

        public String getFolderColorRgb() {
            return folderColorRgb;
        }

        public void setFolderColorRgb(String folderColorRgb) {
            this.folderColorRgb = folderColorRgb;
        }

        public String getOriginalFilename() {
            return originalFilename;
        }

        public void setOriginalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
        }

        public String getFullFileExtension() {
            return fullFileExtension;
        }

        public void setFullFileExtension(String fullFileExtension) {
            this.fullFileExtension = fullFileExtension;
        }

        public String getFileExtension() {
            return fileExtension;
        }

        public void setFileExtension(String fileExtension) {
            this.fileExtension = fileExtension;
        }

        public String getMd5Checksum() {
            return md5Checksum;
        }

        public void setMd5Checksum(String md5Checksum) {
            this.md5Checksum = md5Checksum;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public Integer getQuotaBytesUsed() {
            return quotaBytesUsed;
        }

        public void setQuotaBytesUsed(Integer quotaBytesUsed) {
            this.quotaBytesUsed = quotaBytesUsed;
        }

        public String getHeadRevisionId() {
            return headRevisionId;
        }

        public void setHeadRevisionId(String headRevisionId) {
            this.headRevisionId = headRevisionId;
        }

        public ContentHints getContentHints() {
            return contentHints;
        }

        public void setContentHints(ContentHints contentHints) {
            this.contentHints = contentHints;
        }

        public ImageMediaMetadata getImageMediaMetadata() {
            return imageMediaMetadata;
        }

        public void setImageMediaMetadata(ImageMediaMetadata imageMediaMetadata) {
            this.imageMediaMetadata = imageMediaMetadata;
        }

        public VideoMediaMetadata getVideoMediaMetadata() {
            return videoMediaMetadata;
        }

        public void setVideoMediaMetadata(VideoMediaMetadata videoMediaMetadata) {
            this.videoMediaMetadata = videoMediaMetadata;
        }

        public Boolean getIsAppAuthorized() {
            return isAppAuthorized;
        }

        public void setIsAppAuthorized(Boolean isAppAuthorized) {
            this.isAppAuthorized = isAppAuthorized;
        }

        public ExportLinks getExportLinks() {
            return exportLinks;
        }

        public void setExportLinks(ExportLinks exportLinks) {
            this.exportLinks = exportLinks;
        }

        public ShortcutDetails getShortcutDetails() {
            return shortcutDetails;
        }

        public void setShortcutDetails(ShortcutDetails shortcutDetails) {
            this.shortcutDetails = shortcutDetails;
        }

    }


    @SuppressWarnings("unused")
    public static class AppProperties {

        @SerializedName("(key)")
        @Expose
        private String key;

        /**
         * No args constructor for use in serialization
         */
        public AppProperties() {
        }

        public AppProperties(String key) {
            super();
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

    }


    @SuppressWarnings("unused")
    public static class Capabilities {

        @SerializedName("canAddChildren")
        @Expose
        private Boolean canAddChildren;
        @SerializedName("canAddFolderFromAnotherDrive")
        @Expose
        private Boolean canAddFolderFromAnotherDrive;
        @SerializedName("canAddMyDriveParent")
        @Expose
        private Boolean canAddMyDriveParent;
        @SerializedName("canChangeCopyRequiresWriterPermission")
        @Expose
        private Boolean canChangeCopyRequiresWriterPermission;
        @SerializedName("canChangeViewersCanCopyContent")
        @Expose
        private Boolean canChangeViewersCanCopyContent;
        @SerializedName("canComment")
        @Expose
        private Boolean canComment;
        @SerializedName("canCopy")
        @Expose
        private Boolean canCopy;
        @SerializedName("canDelete")
        @Expose
        private Boolean canDelete;
        @SerializedName("canDeleteChildren")
        @Expose
        private Boolean canDeleteChildren;
        @SerializedName("canDownload")
        @Expose
        private Boolean canDownload;
        @SerializedName("canEdit")
        @Expose
        private Boolean canEdit;
        @SerializedName("canListChildren")
        @Expose
        private Boolean canListChildren;
        @SerializedName("canModifyContent")
        @Expose
        private Boolean canModifyContent;
        @SerializedName("canMoveChildrenOutOfTeamDrive")
        @Expose
        private Boolean canMoveChildrenOutOfTeamDrive;
        @SerializedName("canMoveChildrenOutOfDrive")
        @Expose
        private Boolean canMoveChildrenOutOfDrive;
        @SerializedName("canMoveChildrenWithinTeamDrive")
        @Expose
        private Boolean canMoveChildrenWithinTeamDrive;
        @SerializedName("canMoveChildrenWithinDrive")
        @Expose
        private Boolean canMoveChildrenWithinDrive;
        @SerializedName("canMoveItemIntoTeamDrive")
        @Expose
        private Boolean canMoveItemIntoTeamDrive;
        @SerializedName("canMoveItemOutOfTeamDrive")
        @Expose
        private Boolean canMoveItemOutOfTeamDrive;
        @SerializedName("canMoveItemOutOfDrive")
        @Expose
        private Boolean canMoveItemOutOfDrive;
        @SerializedName("canMoveItemWithinTeamDrive")
        @Expose
        private Boolean canMoveItemWithinTeamDrive;
        @SerializedName("canMoveItemWithinDrive")
        @Expose
        private Boolean canMoveItemWithinDrive;
        @SerializedName("canMoveTeamDriveItem")
        @Expose
        private Boolean canMoveTeamDriveItem;
        @SerializedName("canReadRevisions")
        @Expose
        private Boolean canReadRevisions;
        @SerializedName("canReadTeamDrive")
        @Expose
        private Boolean canReadTeamDrive;
        @SerializedName("canReadDrive")
        @Expose
        private Boolean canReadDrive;
        @SerializedName("canRemoveChildren")
        @Expose
        private Boolean canRemoveChildren;
        @SerializedName("canRemoveMyDriveParent")
        @Expose
        private Boolean canRemoveMyDriveParent;
        @SerializedName("canRename")
        @Expose
        private Boolean canRename;
        @SerializedName("canShare")
        @Expose
        private Boolean canShare;
        @SerializedName("canTrash")
        @Expose
        private Boolean canTrash;
        @SerializedName("canTrashChildren")
        @Expose
        private Boolean canTrashChildren;
        @SerializedName("canUntrash")
        @Expose
        private Boolean canUntrash;

        /**
         * No args constructor for use in serialization
         */
        public Capabilities() {
        }

        public Capabilities(Boolean canAddChildren, Boolean canAddFolderFromAnotherDrive, Boolean canAddMyDriveParent, Boolean canChangeCopyRequiresWriterPermission, Boolean canChangeViewersCanCopyContent, Boolean canComment, Boolean canCopy, Boolean canDelete, Boolean canDeleteChildren, Boolean canDownload, Boolean canEdit, Boolean canListChildren, Boolean canModifyContent, Boolean canMoveChildrenOutOfTeamDrive, Boolean canMoveChildrenOutOfDrive, Boolean canMoveChildrenWithinTeamDrive, Boolean canMoveChildrenWithinDrive, Boolean canMoveItemIntoTeamDrive, Boolean canMoveItemOutOfTeamDrive, Boolean canMoveItemOutOfDrive, Boolean canMoveItemWithinTeamDrive, Boolean canMoveItemWithinDrive, Boolean canMoveTeamDriveItem, Boolean canReadRevisions, Boolean canReadTeamDrive, Boolean canReadDrive, Boolean canRemoveChildren, Boolean canRemoveMyDriveParent, Boolean canRename, Boolean canShare, Boolean canTrash, Boolean canTrashChildren, Boolean canUntrash) {
            super();
            this.canAddChildren = canAddChildren;
            this.canAddFolderFromAnotherDrive = canAddFolderFromAnotherDrive;
            this.canAddMyDriveParent = canAddMyDriveParent;
            this.canChangeCopyRequiresWriterPermission = canChangeCopyRequiresWriterPermission;
            this.canChangeViewersCanCopyContent = canChangeViewersCanCopyContent;
            this.canComment = canComment;
            this.canCopy = canCopy;
            this.canDelete = canDelete;
            this.canDeleteChildren = canDeleteChildren;
            this.canDownload = canDownload;
            this.canEdit = canEdit;
            this.canListChildren = canListChildren;
            this.canModifyContent = canModifyContent;
            this.canMoveChildrenOutOfTeamDrive = canMoveChildrenOutOfTeamDrive;
            this.canMoveChildrenOutOfDrive = canMoveChildrenOutOfDrive;
            this.canMoveChildrenWithinTeamDrive = canMoveChildrenWithinTeamDrive;
            this.canMoveChildrenWithinDrive = canMoveChildrenWithinDrive;
            this.canMoveItemIntoTeamDrive = canMoveItemIntoTeamDrive;
            this.canMoveItemOutOfTeamDrive = canMoveItemOutOfTeamDrive;
            this.canMoveItemOutOfDrive = canMoveItemOutOfDrive;
            this.canMoveItemWithinTeamDrive = canMoveItemWithinTeamDrive;
            this.canMoveItemWithinDrive = canMoveItemWithinDrive;
            this.canMoveTeamDriveItem = canMoveTeamDriveItem;
            this.canReadRevisions = canReadRevisions;
            this.canReadTeamDrive = canReadTeamDrive;
            this.canReadDrive = canReadDrive;
            this.canRemoveChildren = canRemoveChildren;
            this.canRemoveMyDriveParent = canRemoveMyDriveParent;
            this.canRename = canRename;
            this.canShare = canShare;
            this.canTrash = canTrash;
            this.canTrashChildren = canTrashChildren;
            this.canUntrash = canUntrash;
        }

        public Boolean getCanAddChildren() {
            return canAddChildren;
        }

        public void setCanAddChildren(Boolean canAddChildren) {
            this.canAddChildren = canAddChildren;
        }

        public Boolean getCanAddFolderFromAnotherDrive() {
            return canAddFolderFromAnotherDrive;
        }

        public void setCanAddFolderFromAnotherDrive(Boolean canAddFolderFromAnotherDrive) {
            this.canAddFolderFromAnotherDrive = canAddFolderFromAnotherDrive;
        }

        public Boolean getCanAddMyDriveParent() {
            return canAddMyDriveParent;
        }

        public void setCanAddMyDriveParent(Boolean canAddMyDriveParent) {
            this.canAddMyDriveParent = canAddMyDriveParent;
        }

        public Boolean getCanChangeCopyRequiresWriterPermission() {
            return canChangeCopyRequiresWriterPermission;
        }

        public void setCanChangeCopyRequiresWriterPermission(Boolean canChangeCopyRequiresWriterPermission) {
            this.canChangeCopyRequiresWriterPermission = canChangeCopyRequiresWriterPermission;
        }

        public Boolean getCanChangeViewersCanCopyContent() {
            return canChangeViewersCanCopyContent;
        }

        public void setCanChangeViewersCanCopyContent(Boolean canChangeViewersCanCopyContent) {
            this.canChangeViewersCanCopyContent = canChangeViewersCanCopyContent;
        }

        public Boolean getCanComment() {
            return canComment;
        }

        public void setCanComment(Boolean canComment) {
            this.canComment = canComment;
        }

        public Boolean getCanCopy() {
            return canCopy;
        }

        public void setCanCopy(Boolean canCopy) {
            this.canCopy = canCopy;
        }

        public Boolean getCanDelete() {
            return canDelete;
        }

        public void setCanDelete(Boolean canDelete) {
            this.canDelete = canDelete;
        }

        public Boolean getCanDeleteChildren() {
            return canDeleteChildren;
        }

        public void setCanDeleteChildren(Boolean canDeleteChildren) {
            this.canDeleteChildren = canDeleteChildren;
        }

        public Boolean getCanDownload() {
            return canDownload;
        }

        public void setCanDownload(Boolean canDownload) {
            this.canDownload = canDownload;
        }

        public Boolean getCanEdit() {
            return canEdit;
        }

        public void setCanEdit(Boolean canEdit) {
            this.canEdit = canEdit;
        }

        public Boolean getCanListChildren() {
            return canListChildren;
        }

        public void setCanListChildren(Boolean canListChildren) {
            this.canListChildren = canListChildren;
        }

        public Boolean getCanModifyContent() {
            return canModifyContent;
        }

        public void setCanModifyContent(Boolean canModifyContent) {
            this.canModifyContent = canModifyContent;
        }

        public Boolean getCanMoveChildrenOutOfTeamDrive() {
            return canMoveChildrenOutOfTeamDrive;
        }

        public void setCanMoveChildrenOutOfTeamDrive(Boolean canMoveChildrenOutOfTeamDrive) {
            this.canMoveChildrenOutOfTeamDrive = canMoveChildrenOutOfTeamDrive;
        }

        public Boolean getCanMoveChildrenOutOfDrive() {
            return canMoveChildrenOutOfDrive;
        }

        public void setCanMoveChildrenOutOfDrive(Boolean canMoveChildrenOutOfDrive) {
            this.canMoveChildrenOutOfDrive = canMoveChildrenOutOfDrive;
        }

        public Boolean getCanMoveChildrenWithinTeamDrive() {
            return canMoveChildrenWithinTeamDrive;
        }

        public void setCanMoveChildrenWithinTeamDrive(Boolean canMoveChildrenWithinTeamDrive) {
            this.canMoveChildrenWithinTeamDrive = canMoveChildrenWithinTeamDrive;
        }

        public Boolean getCanMoveChildrenWithinDrive() {
            return canMoveChildrenWithinDrive;
        }

        public void setCanMoveChildrenWithinDrive(Boolean canMoveChildrenWithinDrive) {
            this.canMoveChildrenWithinDrive = canMoveChildrenWithinDrive;
        }

        public Boolean getCanMoveItemIntoTeamDrive() {
            return canMoveItemIntoTeamDrive;
        }

        public void setCanMoveItemIntoTeamDrive(Boolean canMoveItemIntoTeamDrive) {
            this.canMoveItemIntoTeamDrive = canMoveItemIntoTeamDrive;
        }

        public Boolean getCanMoveItemOutOfTeamDrive() {
            return canMoveItemOutOfTeamDrive;
        }

        public void setCanMoveItemOutOfTeamDrive(Boolean canMoveItemOutOfTeamDrive) {
            this.canMoveItemOutOfTeamDrive = canMoveItemOutOfTeamDrive;
        }

        public Boolean getCanMoveItemOutOfDrive() {
            return canMoveItemOutOfDrive;
        }

        public void setCanMoveItemOutOfDrive(Boolean canMoveItemOutOfDrive) {
            this.canMoveItemOutOfDrive = canMoveItemOutOfDrive;
        }

        public Boolean getCanMoveItemWithinTeamDrive() {
            return canMoveItemWithinTeamDrive;
        }

        public void setCanMoveItemWithinTeamDrive(Boolean canMoveItemWithinTeamDrive) {
            this.canMoveItemWithinTeamDrive = canMoveItemWithinTeamDrive;
        }

        public Boolean getCanMoveItemWithinDrive() {
            return canMoveItemWithinDrive;
        }

        public void setCanMoveItemWithinDrive(Boolean canMoveItemWithinDrive) {
            this.canMoveItemWithinDrive = canMoveItemWithinDrive;
        }

        public Boolean getCanMoveTeamDriveItem() {
            return canMoveTeamDriveItem;
        }

        public void setCanMoveTeamDriveItem(Boolean canMoveTeamDriveItem) {
            this.canMoveTeamDriveItem = canMoveTeamDriveItem;
        }

        public Boolean getCanReadRevisions() {
            return canReadRevisions;
        }

        public void setCanReadRevisions(Boolean canReadRevisions) {
            this.canReadRevisions = canReadRevisions;
        }

        public Boolean getCanReadTeamDrive() {
            return canReadTeamDrive;
        }

        public void setCanReadTeamDrive(Boolean canReadTeamDrive) {
            this.canReadTeamDrive = canReadTeamDrive;
        }

        public Boolean getCanReadDrive() {
            return canReadDrive;
        }

        public void setCanReadDrive(Boolean canReadDrive) {
            this.canReadDrive = canReadDrive;
        }

        public Boolean getCanRemoveChildren() {
            return canRemoveChildren;
        }

        public void setCanRemoveChildren(Boolean canRemoveChildren) {
            this.canRemoveChildren = canRemoveChildren;
        }

        public Boolean getCanRemoveMyDriveParent() {
            return canRemoveMyDriveParent;
        }

        public void setCanRemoveMyDriveParent(Boolean canRemoveMyDriveParent) {
            this.canRemoveMyDriveParent = canRemoveMyDriveParent;
        }

        public Boolean getCanRename() {
            return canRename;
        }

        public void setCanRename(Boolean canRename) {
            this.canRename = canRename;
        }

        public Boolean getCanShare() {
            return canShare;
        }

        public void setCanShare(Boolean canShare) {
            this.canShare = canShare;
        }

        public Boolean getCanTrash() {
            return canTrash;
        }

        public void setCanTrash(Boolean canTrash) {
            this.canTrash = canTrash;
        }

        public Boolean getCanTrashChildren() {
            return canTrashChildren;
        }

        public void setCanTrashChildren(Boolean canTrashChildren) {
            this.canTrashChildren = canTrashChildren;
        }

        public Boolean getCanUntrash() {
            return canUntrash;
        }

        public void setCanUntrash(Boolean canUntrash) {
            this.canUntrash = canUntrash;
        }

    }


    @SuppressWarnings("unused")
    public static class ContentHints {

        @SerializedName("thumbnail")
        @Expose
        private Thumbnail thumbnail;
        @SerializedName("indexableText")
        @Expose
        private String indexableText;

        /**
         * No args constructor for use in serialization
         */
        public ContentHints() {
        }

        public ContentHints(Thumbnail thumbnail, String indexableText) {
            super();
            this.thumbnail = thumbnail;
            this.indexableText = indexableText;
        }

        public Thumbnail getThumbnail() {
            return thumbnail;
        }

        public void setThumbnail(Thumbnail thumbnail) {
            this.thumbnail = thumbnail;
        }

        public String getIndexableText() {
            return indexableText;
        }

        public void setIndexableText(String indexableText) {
            this.indexableText = indexableText;
        }

    }

    @SuppressWarnings("unused")
    public static class ExportLinks {

        @SerializedName("(key)")
        @Expose
        private String key;

        /**
         * No args constructor for use in serialization
         */
        public ExportLinks() {
        }

        public ExportLinks(String key) {
            super();
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

    }


    @SuppressWarnings("unused")
    public static class ImageMediaMetadata {

        @SerializedName("width")
        @Expose
        private Integer width;
        @SerializedName("height")
        @Expose
        private Integer height;
        @SerializedName("rotation")
        @Expose
        private Integer rotation;
        @SerializedName("location")
        @Expose
        private Location location;
        @SerializedName("time")
        @Expose
        private String time;
        @SerializedName("cameraMake")
        @Expose
        private String cameraMake;
        @SerializedName("cameraModel")
        @Expose
        private String cameraModel;
        @SerializedName("exposureTime")
        @Expose
        private Double exposureTime;
        @SerializedName("aperture")
        @Expose
        private Double aperture;
        @SerializedName("flashUsed")
        @Expose
        private Boolean flashUsed;
        @SerializedName("focalLength")
        @Expose
        private Double focalLength;
        @SerializedName("isoSpeed")
        @Expose
        private Integer isoSpeed;
        @SerializedName("meteringMode")
        @Expose
        private String meteringMode;
        @SerializedName("sensor")
        @Expose
        private String sensor;
        @SerializedName("exposureMode")
        @Expose
        private String exposureMode;
        @SerializedName("colorSpace")
        @Expose
        private String colorSpace;
        @SerializedName("whiteBalance")
        @Expose
        private String whiteBalance;
        @SerializedName("exposureBias")
        @Expose
        private Double exposureBias;
        @SerializedName("maxApertureValue")
        @Expose
        private Double maxApertureValue;
        @SerializedName("subjectDistance")
        @Expose
        private Integer subjectDistance;
        @SerializedName("lens")
        @Expose
        private String lens;

        /**
         * No args constructor for use in serialization
         */
        public ImageMediaMetadata() {
        }

        public ImageMediaMetadata(Integer width, Integer height, Integer rotation, Location location, String time, String cameraMake, String cameraModel, Double exposureTime, Double aperture, Boolean flashUsed, Double focalLength, Integer isoSpeed, String meteringMode, String sensor, String exposureMode, String colorSpace, String whiteBalance, Double exposureBias, Double maxApertureValue, Integer subjectDistance, String lens) {
            super();
            this.width = width;
            this.height = height;
            this.rotation = rotation;
            this.location = location;
            this.time = time;
            this.cameraMake = cameraMake;
            this.cameraModel = cameraModel;
            this.exposureTime = exposureTime;
            this.aperture = aperture;
            this.flashUsed = flashUsed;
            this.focalLength = focalLength;
            this.isoSpeed = isoSpeed;
            this.meteringMode = meteringMode;
            this.sensor = sensor;
            this.exposureMode = exposureMode;
            this.colorSpace = colorSpace;
            this.whiteBalance = whiteBalance;
            this.exposureBias = exposureBias;
            this.maxApertureValue = maxApertureValue;
            this.subjectDistance = subjectDistance;
            this.lens = lens;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

        public Integer getHeight() {
            return height;
        }

        public void setHeight(Integer height) {
            this.height = height;
        }

        public Integer getRotation() {
            return rotation;
        }

        public void setRotation(Integer rotation) {
            this.rotation = rotation;
        }

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getCameraMake() {
            return cameraMake;
        }

        public void setCameraMake(String cameraMake) {
            this.cameraMake = cameraMake;
        }

        public String getCameraModel() {
            return cameraModel;
        }

        public void setCameraModel(String cameraModel) {
            this.cameraModel = cameraModel;
        }

        public Double getExposureTime() {
            return exposureTime;
        }

        public void setExposureTime(Double exposureTime) {
            this.exposureTime = exposureTime;
        }

        public Double getAperture() {
            return aperture;
        }

        public void setAperture(Double aperture) {
            this.aperture = aperture;
        }

        public Boolean getFlashUsed() {
            return flashUsed;
        }

        public void setFlashUsed(Boolean flashUsed) {
            this.flashUsed = flashUsed;
        }

        public Double getFocalLength() {
            return focalLength;
        }

        public void setFocalLength(Double focalLength) {
            this.focalLength = focalLength;
        }

        public Integer getIsoSpeed() {
            return isoSpeed;
        }

        public void setIsoSpeed(Integer isoSpeed) {
            this.isoSpeed = isoSpeed;
        }

        public String getMeteringMode() {
            return meteringMode;
        }

        public void setMeteringMode(String meteringMode) {
            this.meteringMode = meteringMode;
        }

        public String getSensor() {
            return sensor;
        }

        public void setSensor(String sensor) {
            this.sensor = sensor;
        }

        public String getExposureMode() {
            return exposureMode;
        }

        public void setExposureMode(String exposureMode) {
            this.exposureMode = exposureMode;
        }

        public String getColorSpace() {
            return colorSpace;
        }

        public void setColorSpace(String colorSpace) {
            this.colorSpace = colorSpace;
        }

        public String getWhiteBalance() {
            return whiteBalance;
        }

        public void setWhiteBalance(String whiteBalance) {
            this.whiteBalance = whiteBalance;
        }

        public Double getExposureBias() {
            return exposureBias;
        }

        public void setExposureBias(Double exposureBias) {
            this.exposureBias = exposureBias;
        }

        public Double getMaxApertureValue() {
            return maxApertureValue;
        }

        public void setMaxApertureValue(Double maxApertureValue) {
            this.maxApertureValue = maxApertureValue;
        }

        public Integer getSubjectDistance() {
            return subjectDistance;
        }

        public void setSubjectDistance(Integer subjectDistance) {
            this.subjectDistance = subjectDistance;
        }

        public String getLens() {
            return lens;
        }

        public void setLens(String lens) {
            this.lens = lens;
        }

    }


    @SuppressWarnings("unused")
    public static class LastModifyingUser {

        @SerializedName("kind")
        @Expose
        private String kind;
        @SerializedName("displayName")
        @Expose
        private String displayName;
        @SerializedName("photoLink")
        @Expose
        private String photoLink;
        @SerializedName("me")
        @Expose
        private Boolean me;
        @SerializedName("permissionId")
        @Expose
        private String permissionId;
        @SerializedName("emailAddress")
        @Expose
        private String emailAddress;

        /**
         * No args constructor for use in serialization
         */
        public LastModifyingUser() {
        }

        public LastModifyingUser(String kind, String displayName, String photoLink, Boolean me, String permissionId, String emailAddress) {
            super();
            this.kind = kind;
            this.displayName = displayName;
            this.photoLink = photoLink;
            this.me = me;
            this.permissionId = permissionId;
            this.emailAddress = emailAddress;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getPhotoLink() {
            return photoLink;
        }

        public void setPhotoLink(String photoLink) {
            this.photoLink = photoLink;
        }

        public Boolean getMe() {
            return me;
        }

        public void setMe(Boolean me) {
            this.me = me;
        }

        public String getPermissionId() {
            return permissionId;
        }

        public void setPermissionId(String permissionId) {
            this.permissionId = permissionId;
        }

        public String getEmailAddress() {
            return emailAddress;
        }

        public void setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
        }

    }


    @SuppressWarnings("unused")
    public static class Location {

        @SerializedName("latitude")
        @Expose
        private Double latitude;
        @SerializedName("longitude")
        @Expose
        private Double longitude;
        @SerializedName("altitude")
        @Expose
        private Double altitude;

        /**
         * No args constructor for use in serialization
         */
        public Location() {
        }

        public Location(Double latitude, Double longitude, Double altitude) {
            super();
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public Double getAltitude() {
            return altitude;
        }

        public void setAltitude(Double altitude) {
            this.altitude = altitude;
        }

    }


    @SuppressWarnings("unused")
    public static class Owner {

        @SerializedName("kind")
        @Expose
        private String kind;
        @SerializedName("displayName")
        @Expose
        private String displayName;
        @SerializedName("photoLink")
        @Expose
        private String photoLink;
        @SerializedName("me")
        @Expose
        private Boolean me;
        @SerializedName("permissionId")
        @Expose
        private String permissionId;
        @SerializedName("emailAddress")
        @Expose
        private String emailAddress;

        /**
         * No args constructor for use in serialization
         */
        public Owner() {
        }

        public Owner(String kind, String displayName, String photoLink, Boolean me, String permissionId, String emailAddress) {
            super();
            this.kind = kind;
            this.displayName = displayName;
            this.photoLink = photoLink;
            this.me = me;
            this.permissionId = permissionId;
            this.emailAddress = emailAddress;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getPhotoLink() {
            return photoLink;
        }

        public void setPhotoLink(String photoLink) {
            this.photoLink = photoLink;
        }

        public Boolean getMe() {
            return me;
        }

        public void setMe(Boolean me) {
            this.me = me;
        }

        public String getPermissionId() {
            return permissionId;
        }

        public void setPermissionId(String permissionId) {
            this.permissionId = permissionId;
        }

        public String getEmailAddress() {
            return emailAddress;
        }

        public void setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
        }

    }


    @SuppressWarnings("unused")
    public static class Properties {

        @SerializedName("(key)")
        @Expose
        private String key;

        /**
         * No args constructor for use in serialization
         */
        public Properties() {
        }

        public Properties(String key) {
            super();
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

    }


    @SuppressWarnings("unused")
    public static class SharingUser {

        @SerializedName("kind")
        @Expose
        private String kind;
        @SerializedName("displayName")
        @Expose
        private String displayName;
        @SerializedName("photoLink")
        @Expose
        private String photoLink;
        @SerializedName("me")
        @Expose
        private Boolean me;
        @SerializedName("permissionId")
        @Expose
        private String permissionId;
        @SerializedName("emailAddress")
        @Expose
        private String emailAddress;

        /**
         * No args constructor for use in serialization
         */
        public SharingUser() {
        }

        public SharingUser(String kind, String displayName, String photoLink, Boolean me, String permissionId, String emailAddress) {
            super();
            this.kind = kind;
            this.displayName = displayName;
            this.photoLink = photoLink;
            this.me = me;
            this.permissionId = permissionId;
            this.emailAddress = emailAddress;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getPhotoLink() {
            return photoLink;
        }

        public void setPhotoLink(String photoLink) {
            this.photoLink = photoLink;
        }

        public Boolean getMe() {
            return me;
        }

        public void setMe(Boolean me) {
            this.me = me;
        }

        public String getPermissionId() {
            return permissionId;
        }

        public void setPermissionId(String permissionId) {
            this.permissionId = permissionId;
        }

        public String getEmailAddress() {
            return emailAddress;
        }

        public void setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
        }

    }

    @SuppressWarnings("unused")
    public static class ShortcutDetails {

        @SerializedName("targetId")
        @Expose
        private String targetId;
        @SerializedName("targetMimeType")
        @Expose
        private String targetMimeType;

        /**
         * No args constructor for use in serialization
         */
        public ShortcutDetails() {
        }

        public ShortcutDetails(String targetId, String targetMimeType) {
            super();
            this.targetId = targetId;
            this.targetMimeType = targetMimeType;
        }

        public String getTargetId() {
            return targetId;
        }

        public void setTargetId(String targetId) {
            this.targetId = targetId;
        }

        public String getTargetMimeType() {
            return targetMimeType;
        }

        public void setTargetMimeType(String targetMimeType) {
            this.targetMimeType = targetMimeType;
        }

    }

    @SuppressWarnings("unused")
    public static class Thumbnail {

        @SerializedName("image")
        @Expose
        private String image;
        @SerializedName("mimeType")
        @Expose
        private String mimeType;

        /**
         * No args constructor for use in serialization
         */
        public Thumbnail() {
        }

        public Thumbnail(String image, String mimeType) {
            super();
            this.image = image;
            this.mimeType = mimeType;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

    }

    @SuppressWarnings("unused")
    public static class TrashingUser {

        @SerializedName("kind")
        @Expose
        private String kind;
        @SerializedName("displayName")
        @Expose
        private String displayName;
        @SerializedName("photoLink")
        @Expose
        private String photoLink;
        @SerializedName("me")
        @Expose
        private Boolean me;
        @SerializedName("permissionId")
        @Expose
        private String permissionId;
        @SerializedName("emailAddress")
        @Expose
        private String emailAddress;

        /**
         * No args constructor for use in serialization
         */
        public TrashingUser() {
        }

        public TrashingUser(String kind, String displayName, String photoLink, Boolean me, String permissionId, String emailAddress) {
            super();
            this.kind = kind;
            this.displayName = displayName;
            this.photoLink = photoLink;
            this.me = me;
            this.permissionId = permissionId;
            this.emailAddress = emailAddress;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getPhotoLink() {
            return photoLink;
        }

        public void setPhotoLink(String photoLink) {
            this.photoLink = photoLink;
        }

        public Boolean getMe() {
            return me;
        }

        public void setMe(Boolean me) {
            this.me = me;
        }

        public String getPermissionId() {
            return permissionId;
        }

        public void setPermissionId(String permissionId) {
            this.permissionId = permissionId;
        }

        public String getEmailAddress() {
            return emailAddress;
        }

        public void setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
        }

    }

    @SuppressWarnings("unused")
    public static class VideoMediaMetadata {

        @SerializedName("width")
        @Expose
        private Integer width;
        @SerializedName("height")
        @Expose
        private Integer height;
        @SerializedName("durationMillis")
        @Expose
        private Integer durationMillis;

        /**
         * No args constructor for use in serialization
         */
        public VideoMediaMetadata() {
        }

        public VideoMediaMetadata(Integer width, Integer height, Integer durationMillis) {
            super();
            this.width = width;
            this.height = height;
            this.durationMillis = durationMillis;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

        public Integer getHeight() {
            return height;
        }

        public void setHeight(Integer height) {
            this.height = height;
        }

        public Integer getDurationMillis() {
            return durationMillis;
        }

        public void setDurationMillis(Integer durationMillis) {
            this.durationMillis = durationMillis;
        }
    }

    // Permissions Resource
    // https://developers.google.com/drive/api/v3/reference/permissions#resource
    // Example :
    //
    // {
    //  "kind": "drive#permission",
    //  "id": "hello",
    //  "type": "hello",
    //  "emailAddress": "hello",
    //  "domain": "hello",
    //  "role": "hello",
    //  "allowFileDiscovery": false,
    //  "displayName": "hello",
    //  "photoLink": "hello",
    //  "expirationTime": "2014-01-01T23:28:56.782Z",
    //  "teamDrivePermissionDetails": [
    //    {
    //      "teamDrivePermissionType": "hello",
    //      "role": "hello",
    //      "inheritedFrom": "hello",
    //      "inherited": false
    //    }
    //  ],
    //  "permissionDetails": [
    //    {
    //      "permissionType": "hello",
    //      "role": "hello",
    //      "inheritedFrom": "hello",
    //      "inherited": false
    //    }
    //  ],
    //  "deleted": false
    //}


    @SuppressWarnings("unused")
    public static class PermissionsResource {

        @SerializedName("kind")
        @Expose
        private String kind;
        @SerializedName("id")
        @Expose
        private String id;
        @SerializedName("type")
        @Expose
        private String type;
        @SerializedName("emailAddress")
        @Expose
        private String emailAddress;
        @SerializedName("domain")
        @Expose
        private String domain;
        @SerializedName("role")
        @Expose
        private String role;
        @SerializedName("allowFileDiscovery")
        @Expose
        private Boolean allowFileDiscovery;
        @SerializedName("displayName")
        @Expose
        private String displayName;
        @SerializedName("photoLink")
        @Expose
        private String photoLink;
        @SerializedName("expirationTime")
        @Expose
        private Date expirationTime;
        @SerializedName("teamDrivePermissionDetails")
        @Expose
        private List<TeamDrivePermissionDetail> teamDrivePermissionDetails = null;
        @SerializedName("permissionDetails")
        @Expose
        private List<PermissionDetail> permissionDetails = null;
        @SerializedName("deleted")
        @Expose
        private Boolean deleted;

        /**
         * No args constructor for use in serialization
         */
        public PermissionsResource() {
        }


        public PermissionsResource(String kind, String id, String type, String emailAddress, String domain, String role, Boolean allowFileDiscovery, String displayName, String photoLink, Date expirationTime, List<TeamDrivePermissionDetail> teamDrivePermissionDetails, List<PermissionDetail> permissionDetails, Boolean deleted) {
            super();
            this.kind = kind;
            this.id = id;
            this.type = type;
            this.emailAddress = emailAddress;
            this.domain = domain;
            this.role = role;
            this.allowFileDiscovery = allowFileDiscovery;
            this.displayName = displayName;
            this.photoLink = photoLink;
            this.expirationTime = expirationTime;
            this.teamDrivePermissionDetails = teamDrivePermissionDetails;
            this.permissionDetails = permissionDetails;
            this.deleted = deleted;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getEmailAddress() {
            return emailAddress;
        }

        public void setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public Boolean getAllowFileDiscovery() {
            return allowFileDiscovery;
        }

        public void setAllowFileDiscovery(Boolean allowFileDiscovery) {
            this.allowFileDiscovery = allowFileDiscovery;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getPhotoLink() {
            return photoLink;
        }

        public void setPhotoLink(String photoLink) {
            this.photoLink = photoLink;
        }

        public Date getExpirationTime() {
            return expirationTime;
        }

        public void setExpirationTime(Date expirationTime) {
            this.expirationTime = expirationTime;
        }

        public List<TeamDrivePermissionDetail> getTeamDrivePermissionDetails() {
            return teamDrivePermissionDetails;
        }

        public void setTeamDrivePermissionDetails(List<TeamDrivePermissionDetail> teamDrivePermissionDetails) {
            this.teamDrivePermissionDetails = teamDrivePermissionDetails;
        }

        public List<PermissionDetail> getPermissionDetails() {
            return permissionDetails;
        }

        public void setPermissionDetails(List<PermissionDetail> permissionDetails) {
            this.permissionDetails = permissionDetails;
        }

        public Boolean getDeleted() {
            return deleted;
        }

        public void setDeleted(Boolean deleted) {
            this.deleted = deleted;
        }

    }

    @SuppressWarnings("unused")
    public static class PermissionDetail {

        @SerializedName("permissionType")
        @Expose
        private String permissionType;
        @SerializedName("role")
        @Expose
        private String role;
        @SerializedName("inheritedFrom")
        @Expose
        private String inheritedFrom;
        @SerializedName("inherited")
        @Expose
        private Boolean inherited;

        /**
         * No args constructor for use in serialization
         */
        public PermissionDetail() {
        }


        public PermissionDetail(String permissionType, String role, String inheritedFrom, Boolean inherited) {
            super();
            this.permissionType = permissionType;
            this.role = role;
            this.inheritedFrom = inheritedFrom;
            this.inherited = inherited;
        }

        public String getPermissionType() {
            return permissionType;
        }

        public void setPermissionType(String permissionType) {
            this.permissionType = permissionType;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getInheritedFrom() {
            return inheritedFrom;
        }

        public void setInheritedFrom(String inheritedFrom) {
            this.inheritedFrom = inheritedFrom;
        }

        public Boolean getInherited() {
            return inherited;
        }

        public void setInherited(Boolean inherited) {
            this.inherited = inherited;
        }

    }

    @SuppressWarnings("unused")
    public static class TeamDrivePermissionDetail {

        @SerializedName("teamDrivePermissionType")
        @Expose
        private String teamDrivePermissionType;
        @SerializedName("role")
        @Expose
        private String role;
        @SerializedName("inheritedFrom")
        @Expose
        private String inheritedFrom;
        @SerializedName("inherited")
        @Expose
        private Boolean inherited;

        /**
         * No args constructor for use in serialization
         */
        public TeamDrivePermissionDetail() {
        }


        public TeamDrivePermissionDetail(String teamDrivePermissionType, String role, String inheritedFrom, Boolean inherited) {
            super();
            this.teamDrivePermissionType = teamDrivePermissionType;
            this.role = role;
            this.inheritedFrom = inheritedFrom;
            this.inherited = inherited;
        }

        public String getTeamDrivePermissionType() {
            return teamDrivePermissionType;
        }

        public void setTeamDrivePermissionType(String teamDrivePermissionType) {
            this.teamDrivePermissionType = teamDrivePermissionType;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getInheritedFrom() {
            return inheritedFrom;
        }

        public void setInheritedFrom(String inheritedFrom) {
            this.inheritedFrom = inheritedFrom;
        }

        public Boolean getInherited() {
            return inherited;
        }

        public void setInherited(Boolean inherited) {
            this.inherited = inherited;
        }

    }
}
