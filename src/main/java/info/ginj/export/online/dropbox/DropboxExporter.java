package info.ginj.export.online.dropbox;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import info.ginj.Ginj;
import info.ginj.export.online.AbstractOAuth2Exporter;
import info.ginj.export.online.exception.AuthorizationException;
import info.ginj.export.online.exception.CommunicationException;
import info.ginj.export.online.exception.UploadException;
import info.ginj.model.*;
import info.ginj.util.UI;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * Handles interaction with Dropbox
 * see https://www.dropbox.com/developers/documentation/http/documentation
 */
public class DropboxExporter extends AbstractOAuth2Exporter {

    private static final Logger logger = LoggerFactory.getLogger(DropboxExporter.class);

    private static final String DROPBOX_CLIENT_APP_KEY = "pdio3i9brehyjo1";
    private static final String DROPBOX_OAUTH2_AUTH_URL = "https://www.dropbox.com/oauth2/authorize";
    private static final String DROPBOX_OAUTH2_TOKEN_URL = "https://api.dropboxapi.com/oauth2/token";
    private static final String DROPBOX_REVOKE_URL = "https://www.dropbox.com/account/connected_apps";

    public static final String NAME = "Dropbox";


    @Override
    public String getExporterName() {
        return NAME;
    }

    @Override
    protected String getClientAppId() {
        return DROPBOX_CLIENT_APP_KEY;
    }

    @Override
    protected String getSecretAppKey() {
        return null; // Dropbox does not need the secret key
    }

    @Override
    protected String getOAuth2AuthorizeUrl() {
        return DROPBOX_OAUTH2_AUTH_URL;
    }

    @Override
    protected String getAdditionalAuthorizeParam() {
        // Needed to receive a short-lived access token + a refresh token, more future proof logic
        return "&token_access_type=offline";
    }

    @Override
    protected String getOAuth2TokenUrl() {
        return DROPBOX_OAUTH2_TOKEN_URL;
    }

    @Override
    public String getOAuth2RevokeUrl() {
        return DROPBOX_REVOKE_URL;
    }

    @Override
    protected List<String> getRequiredScopes() {
        // For Dropbox, no scope means permissions defined at the app level on the site
        // Future : "account_info.read files.content.write sharing.write"
        return null;
    }

    @Override
    public String getDefaultShareText() {
        return "Add to Dropbox";
    }

    @Override
    public String getIconPath() {
        return "/img/logo/dropbox.png";
    }

    @Override
    public boolean isOnlineService() {
        return true;
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
     * Exports the given capture
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
     * This method checks that Dropbox authorizations are OK by fetching user info
     *
     * @param account the account to validate
     * @throws CommunicationException in case a communication error occurs
     * @throws AuthorizationException in case authorization fails
     */
    @Override
    public void checkAuthorizations(Account account) throws CommunicationException, AuthorizationException {
        logProgress("Checking authorizations", PROGRESS_CHECK_AUTHORIZE_START);

        String accessToken = getAccessToken(account);
        if (accessToken == null) {
            throw new AuthorizationException("No access token was provided");
        }
        if (getProfile(accessToken) == null) {
            throw new AuthorizationException("Received empty username");
        }
    }


    protected Profile getProfile(String accessToken) throws CommunicationException, AuthorizationException {
        CloseableHttpClient client = HttpClients.createDefault();

        HttpPost httpPost;
        try {
            URIBuilder builder = new URIBuilder("https://api.dropboxapi.com/2/users/get_current_account");
            httpPost = new HttpPost(builder.build());
        }
        catch (URISyntaxException e) {
            throw new CommunicationException(e);
        }

        httpPost.addHeader("Authorization", "Bearer " + accessToken);

        try {
            CloseableHttpResponse response = client.execute(httpPost);
            if (isStatusOK(response.getCode())) {
                final String responseText;
                try {
                    responseText = EntityUtils.toString(response.getEntity());
                }
                catch (ParseException e) {
                    throw new CommunicationException("Could not parse account information response as String: " + response.getEntity());
                }
                final AccountInfo accountInfo = new Gson().fromJson(responseText, AccountInfo.class);
                if (accountInfo == null) {
                    throw new CommunicationException("Returned account info is null.");
                }
                Profile profile = new Profile();
                profile.setName(accountInfo.getName().getDisplayName());
                profile.setEmail(accountInfo.getEmail());
                return profile;
            }
            else {
                throw new CommunicationException("The server returned the following error when listing albums:\n" + getResponseError(response));
            }
        }
        catch (IOException e) {
            throw new CommunicationException(e);
        }
    }


    /**
     * Uploads a capture to Dropbox, and optionally shares it and returns the URL of the shared media.
     *
     * @param capture The object representing the captured screenshot or video
     * @param target  the target to export this capture to
     * @return a public URL to share to give access to the uploaded media.
     * @throws AuthorizationException if user has no, or insufficient, authorizations, or if a token error occurs
     * @throws CommunicationException if an url, network or decoding error occurs
     * @throws UploadException        if an upload-specific error occurs
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
        final FileMetadata fileMetadata = uploadFile(client, target, capture);

        if (target.getSettings().getMustShare()) {
            // Step 2: Share it
            SharedLinkMetadata sharedLinkMetadata = shareFile(client, target, fileMetadata.getPathDisplay());

            return new Export(getExporterName(), fileMetadata.getPathDisplay(), sharedLinkMetadata.getUrl(), false);
        }
        else {
            return new Export(getExporterName(), fileMetadata.getPathDisplay(), null, false);
        }
    }

    public FileMetadata uploadFile(CloseableHttpClient client, Target target, Capture capture) throws AuthorizationException, UploadException, CommunicationException {
        FileMetadata fileMetadata;

        String sessionId;

        final File file = capture.getRenderedFile();

        int maxChunkSize = CHUNK_SIZE;
        byte[] buffer = new byte[maxChunkSize];
        int offset = 0;
        long remainingBytes = file.length();

        try (InputStream is = new FileInputStream(file)) {
            // Step 1: Initiating an upload session with the first CHUNK
            logProgress("Uploading", PROGRESS_UPLOAD_START);
            HttpPost httpPost = new HttpPost("https://content.dropboxapi.com/2/files/upload_session/start");

            httpPost.addHeader("Authorization", "Bearer " + getAccessToken(target.getAccount()));
            //httpPost.addHeader("Content-Length", 0); // Don't put it here, it causes a "dupe header" error if there is an entity, and if there is no entity it's forbidden.
            httpPost.addHeader("Dropbox-API-Arg", "{\"close\": false}");

            // First chunk
            int bytesRead = readBytes(is, buffer, maxChunkSize, remainingBytes);
            httpPost.setEntity(new ByteArrayEntity(buffer, 0, bytesRead, ContentType.APPLICATION_OCTET_STREAM));

            // Send request
            try {
                CloseableHttpResponse response = client.execute(httpPost);
                if (isStatusOK(response.getCode())) {
                    final String responseText;
                    try {
                        responseText = EntityUtils.toString(response.getEntity());
                        @SuppressWarnings("rawtypes")
                        Map map = new Gson().fromJson(responseText, Map.class);
                        sessionId = (String) map.get("session_id");
                    }
                    catch (ParseException e) {
                        throw new CommunicationException("Could not parse start upload session response as String: " + response.getEntity());
                    }
                    if (sessionId == null) {
                        throw new CommunicationException("Returned session id is null.");
                    }
                }
                else {
                    final String responseError = getResponseError(response);
                    if ((response.getCode() / 100) == 5) {
                        // Error 5xx
                        // Don't know if Dropbox actually uses it but well
                        throw new UploadException("Resuming not implemented yet:\n" + responseError);
                    }
                    throw new UploadException("The server returned the following error when starting file contents:\n" + responseError);
                }
            }
            catch (IOException e) {
                throw new CommunicationException("Error starting file contents", e);
            }

            // Update counters
            offset += bytesRead;
            remainingBytes = file.length() - offset;


            // Step 2: Append to session with more CHUNKS, if needed
            while (remainingBytes > CHUNK_SIZE) {
                logProgress("Uploading", (int) (PROGRESS_UPLOAD_START + ((PROGRESS_UPLOAD_END - PROGRESS_UPLOAD_START) * offset) / file.length()), offset, file.length());
                httpPost = new HttpPost("https://content.dropboxapi.com/2/files/upload_session/append_v2");

                httpPost.addHeader("Authorization", "Bearer " + getAccessToken(target.getAccount()));
                //httpPost.addHeader("Content-Length", 0); // Don't put it here, it causes a "dupe header" error if there is an entity, and if there is no entity it's forbidden.
                httpPost.addHeader("Dropbox-API-Arg",
                        "{\"cursor\": " +
                                "{\"session_id\": \"" + sessionId + "\"," +
                                "\"offset\": " + offset + "}" +
                                ",\"close\": false}");

                // Next chunk
                bytesRead = readBytes(is, buffer, maxChunkSize, remainingBytes);
                httpPost.setEntity(new ByteArrayEntity(buffer, 0, bytesRead, ContentType.APPLICATION_OCTET_STREAM));

                // Send request
                try {
                    CloseableHttpResponse response = client.execute(httpPost);
                    if (!isStatusOK(response.getCode())) {
                        final String responseError = getResponseError(response);
                        if ((response.getCode() / 100) == 5) {
                            // Error 5xx
                            // Don't know if Dropbox actually uses it but well
                            throw new UploadException("Resuming not implemented yet:\n" + responseError);
                        }
                        throw new UploadException("The server returned the following error when appending file contents:\n" + responseError);
                    }
                }
                catch (IOException e) {
                    throw new CommunicationException("Error appending file contents", e);
                }

                // Update counters
                offset += bytesRead;
                remainingBytes = file.length() - offset;
            }


            // Step 3: Finish session (optionally with the remaining bytes)
            logProgress("Uploading", (int) (PROGRESS_UPLOAD_START + ((PROGRESS_UPLOAD_END - PROGRESS_UPLOAD_START) * offset) / file.length()), offset, file.length());

            final String destinationFileName = "/Applications/" + Ginj.getAppName() + "/" + capture.computeUploadFilename();

            httpPost = new HttpPost("https://content.dropboxapi.com/2/files/upload_session/finish");
            httpPost.addHeader("Authorization", "Bearer " + getAccessToken(target.getAccount()));
            //httpPost.addHeader("Content-Length", 0); // Don't put it here, it causes a "dupe header" error if there is an entity, and if there is no entity it's forbidden.
            httpPost.addHeader("Dropbox-API-Arg",
                    "{\"cursor\": " +
                            "{\"session_id\": \"" + sessionId + "\"," +
                            "\"offset\": " + offset + "}" +
                            ",\"commit\": " +
                            "{\"path\": \"" + destinationFileName + "\"," +
                            "\"mode\": \"add\"," +
                            "\"autorename\": true," +
                            "\"mute\": false," +
                            "\"strict_conflict\": false}" +
                            "}");

            // Last chunk
            bytesRead = readBytes(is, buffer, maxChunkSize, remainingBytes);
            httpPost.setEntity(new ByteArrayEntity(buffer, 0, bytesRead, ContentType.APPLICATION_OCTET_STREAM));

            // Send request
            try {
                CloseableHttpResponse response = client.execute(httpPost);
                if (isStatusOK(response.getCode())) {
                    final String responseText;
                    try {
                        responseText = EntityUtils.toString(response.getEntity());
                        fileMetadata = new Gson().fromJson(responseText, FileMetadata.class);
                        if (fileMetadata == null) {
                            throw new CommunicationException("Returned fileMetadata is null.");
                        }
                    }
                    catch (ParseException e) {
                        throw new CommunicationException("Could not parse finish upload session response as String: " + response.getEntity());
                    }
                }
                else {
                    final String responseError = getResponseError(response);
                    if ((response.getCode() / 100) == 5) {
                        // Error 5xx
                        // Don't know if Dropbox actually uses it but well
                        throw new UploadException("Resuming not implemented yet:\n" + responseError);
                    }
                    throw new UploadException("The server returned the following error when finishing file contents:\n" + responseError);
                }
            }
            catch (IOException e) {
                throw new CommunicationException("Error finishing file contents", e);
            }


            logProgress("Uploaded", PROGRESS_UPLOAD_END, file.length(), file.length());
        }
        catch (FileNotFoundException e) {
            throw new UploadException("File not found: " + file.getAbsolutePath(), e);
        }
        catch (IOException e) {
            throw new UploadException(e);
        }

        return fileMetadata;
    }

    private int readBytes(InputStream is, byte[] buffer, int maxChunkSize, long remainingBytes) throws UploadException {
        int chunkSize = (int) Math.min(maxChunkSize, remainingBytes);
        final int bytesRead;
        try {
            bytesRead = is.read(buffer, 0, chunkSize);
        }
        catch (IOException e) {
            throw new UploadException("Could not read bytes from file");
        }
        return bytesRead;
    }

    public boolean fileExists(CloseableHttpClient client, Target target, String path) throws AuthorizationException, CommunicationException {
        try {
            getFileMetadata(client, target, path);
            return true;
        }
        catch (FileNotFoundException e) {
            return false;
        }
    }

    /**
     * This method implements https://api.dropboxapi.com/2/files/get_metadata
     * @param path
     * @return
     */
    private FileMetadata getFileMetadata(CloseableHttpClient client, Target target, String path) throws AuthorizationException, CommunicationException, FileNotFoundException {
        HttpPost httpPost = new HttpPost("https://api.dropboxapi.com/2/files/get_metadata");

        httpPost.addHeader("Authorization", "Bearer " + getAccessToken(target.getAccount()));
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.setEntity(new StringEntity(
                "{\"path\": \"" + path + "\"," +
                        "\"include_media_info\": false," +
                        "\"include_deleted\": false," +
                        "\"include_has_explicit_shared_members\": false}"

        ));

        // Send request
        CloseableHttpResponse response;
        try {
            response = client.execute(httpPost);
        }
        catch (IOException e) {
            throw new CommunicationException("Error getting metadata", e);
        }
        if (isStatusOK(response.getCode())) {
            final String responseText;
            try {
                responseText = EntityUtils.toString(response.getEntity());
                return new Gson().fromJson(responseText, FileMetadata.class);
            }
            catch (ParseException | IOException e) {
                throw new CommunicationException("Could not parse metadata query response as String: " + response.getEntity());
            }
        }
        else {
            String responseError = getResponseError(response);
            if ("path".equals(responseError)) {
                throw new FileNotFoundException();
            }
            throw new CommunicationException("The server returned the following error when getting metadata:\n" + responseError);
        }
    }

    public SharedLinkMetadata shareFile(CloseableHttpClient client, Target target, String pathDisplay) throws AuthorizationException, CommunicationException {
        HttpPost httpPost = new HttpPost("https://api.dropboxapi.com/2/sharing/create_shared_link_with_settings");

        httpPost.addHeader("Authorization", "Bearer " + getAccessToken(target.getAccount()));
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.setEntity(new StringEntity(
                "{\"path\": \"" + pathDisplay + "\"," +
                        "\"settings\": {" +
                        "\"requested_visibility\": \"public\"," +
                        "\"audience\": \"public\"," +
                        "\"access\": \"viewer\"}" +
                        "}"
        ));

        // Send request
        try {
            CloseableHttpResponse response = client.execute(httpPost);
            if (isStatusOK(response.getCode())) {
                final String responseText;
                try {
                    responseText = EntityUtils.toString(response.getEntity());
                    return new Gson().fromJson(responseText, SharedLinkMetadata.class);
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
     * Dropbox specific version.
     * See https://www.dropbox.com/developers/documentation/http/documentation#error-handling
     * @param httpResponse
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    protected String getResponseError(CloseableHttpResponse httpResponse) {
        int errCode = httpResponse.getCode();
        if (errCode == 409) { // Dropbox endpoint specific error
            try {
                String responseText = EntityUtils.toString(httpResponse.getEntity());
                // Dropbox errors 409 are of the form
                // {"error_summary": "email_not_verified/..", "error": {".tag": "email_not_verified"}}
                try {
                    Map messageMap = new Gson().fromJson(responseText, Map.class);
                    Map errorMap = (Map) messageMap.get("error");
                    return (String) errorMap.get(".tag");
                }
                catch (Exception e) {
                    return httpResponse.getCode() + " (" + responseText + ")";
                }
            }
            catch (IOException | ParseException e) {
                return String.valueOf(errCode);
            }
        }
        else {
            return super.getResponseError(httpResponse);
        }
    }


    ////////////////////////////////////////////////////
    // Autogenerated pojos for (non-Map) Json parsing
    // Created by http://jsonschema2pojo.org
    ////////////////////////////////////////////////////


    // AccountInfo example:
    //
    // {
    //    "account_id": "dbid:AAH4f99T0taONIb-OurWxbNQ6ywGRopQngc",
    //    "name": {
    //        "given_name": "Franz",
    //        "surname": "Ferdinand",
    //        "familiar_name": "Franz",
    //        "display_name": "Franz Ferdinand (Personal)",
    //        "abbreviated_name": "FF"
    //    },
    //    "email": "franz@gmail.com",
    //    "email_verified": false,
    //    "disabled": false,
    //    "locale": "en",
    //    "referral_link": "https://db.tt/ZITNuhtI",
    //    "is_paired": false,
    //    "account_type": {
    //        ".tag": "basic"
    //    },
    //    "root_info": {
    //        ".tag": "user",
    //        "root_namespace_id": "3235641",
    //        "home_namespace_id": "3235641"
    //    },
    //    "profile_photo_url": "https://dl-web.dropbox.com/account_photo/get/dbaphid%3AAAHWGmIXV3sUuOmBfTz0wPsiqHUpBWvv3ZA?vers=1556069330102\u0026size=128x128",
    //    "country": "US"
    //}


    @SuppressWarnings("unused")
    public static class AccountInfo {

        @SerializedName("account_id")
        @Expose
        private String accountId;
        @SerializedName("name")
        @Expose
        private Name name;
        @SerializedName("email")
        @Expose
        private String email;
        @SerializedName("email_verified")
        @Expose
        private Boolean emailVerified;
        @SerializedName("disabled")
        @Expose
        private Boolean disabled;
        @SerializedName("locale")
        @Expose
        private String locale;
        @SerializedName("referral_link")
        @Expose
        private String referralLink;
        @SerializedName("is_paired")
        @Expose
        private Boolean isPaired;
        @SerializedName("account_type")
        @Expose
        private AccountType accountType;
        @SerializedName("root_info")
        @Expose
        private RootInfo rootInfo;
        @SerializedName("profile_photo_url")
        @Expose
        private String profilePhotoUrl;
        @SerializedName("country")
        @Expose
        private String country;

        public AccountInfo() {
        }

        public AccountInfo(String accountId, Name name, String email, Boolean emailVerified, Boolean disabled, String locale, String referralLink, Boolean isPaired, AccountType accountType, RootInfo rootInfo, String profilePhotoUrl, String country) {
            super();
            this.accountId = accountId;
            this.name = name;
            this.email = email;
            this.emailVerified = emailVerified;
            this.disabled = disabled;
            this.locale = locale;
            this.referralLink = referralLink;
            this.isPaired = isPaired;
            this.accountType = accountType;
            this.rootInfo = rootInfo;
            this.profilePhotoUrl = profilePhotoUrl;
            this.country = country;
        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public Name getName() {
            return name;
        }

        public void setName(Name name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Boolean getEmailVerified() {
            return emailVerified;
        }

        public void setEmailVerified(Boolean emailVerified) {
            this.emailVerified = emailVerified;
        }

        public Boolean getDisabled() {
            return disabled;
        }

        public void setDisabled(Boolean disabled) {
            this.disabled = disabled;
        }

        public String getLocale() {
            return locale;
        }

        public void setLocale(String locale) {
            this.locale = locale;
        }

        public String getReferralLink() {
            return referralLink;
        }

        public void setReferralLink(String referralLink) {
            this.referralLink = referralLink;
        }

        public Boolean getIsPaired() {
            return isPaired;
        }

        public void setIsPaired(Boolean isPaired) {
            this.isPaired = isPaired;
        }

        public AccountType getAccountType() {
            return accountType;
        }

        public void setAccountType(AccountType accountType) {
            this.accountType = accountType;
        }

        public RootInfo getRootInfo() {
            return rootInfo;
        }

        public void setRootInfo(RootInfo rootInfo) {
            this.rootInfo = rootInfo;
        }

        public String getProfilePhotoUrl() {
            return profilePhotoUrl;
        }

        public void setProfilePhotoUrl(String profilePhotoUrl) {
            this.profilePhotoUrl = profilePhotoUrl;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

    }


    @SuppressWarnings("unused")
    public static class AccountType {

        @SerializedName(".tag")
        @Expose
        private String tag;

        public AccountType() {
        }

        public AccountType(String tag) {
            super();
            this.tag = tag;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

    }


    @SuppressWarnings("unused")
    public static class Name {

        @SerializedName("given_name")
        @Expose
        private String givenName;
        @SerializedName("surname")
        @Expose
        private String surname;
        @SerializedName("familiar_name")
        @Expose
        private String familiarName;
        @SerializedName("display_name")
        @Expose
        private String displayName;
        @SerializedName("abbreviated_name")
        @Expose
        private String abbreviatedName;

        public Name() {
        }

        public Name(String givenName, String surname, String familiarName, String displayName, String abbreviatedName) {
            super();
            this.givenName = givenName;
            this.surname = surname;
            this.familiarName = familiarName;
            this.displayName = displayName;
            this.abbreviatedName = abbreviatedName;
        }

        public String getGivenName() {
            return givenName;
        }

        public void setGivenName(String givenName) {
            this.givenName = givenName;
        }

        public String getSurname() {
            return surname;
        }

        public void setSurname(String surname) {
            this.surname = surname;
        }

        public String getFamiliarName() {
            return familiarName;
        }

        public void setFamiliarName(String familiarName) {
            this.familiarName = familiarName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getAbbreviatedName() {
            return abbreviatedName;
        }

        public void setAbbreviatedName(String abbreviatedName) {
            this.abbreviatedName = abbreviatedName;
        }

    }


    @SuppressWarnings("unused")
    public static class RootInfo {

        @SerializedName(".tag")
        @Expose
        private String tag;
        @SerializedName("root_namespace_id")
        @Expose
        private String rootNamespaceId;
        @SerializedName("home_namespace_id")
        @Expose
        private String homeNamespaceId;

        public RootInfo() {
        }

        public RootInfo(String tag, String rootNamespaceId, String homeNamespaceId) {
            super();
            this.tag = tag;
            this.rootNamespaceId = rootNamespaceId;
            this.homeNamespaceId = homeNamespaceId;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public String getRootNamespaceId() {
            return rootNamespaceId;
        }

        public void setRootNamespaceId(String rootNamespaceId) {
            this.rootNamespaceId = rootNamespaceId;
        }

        public String getHomeNamespaceId() {
            return homeNamespaceId;
        }

        public void setHomeNamespaceId(String homeNamespaceId) {
            this.homeNamespaceId = homeNamespaceId;
        }

    }

    // FileMetadata example:
    //
    // {
    //    "name": "Prime_Numbers.txt",
    //    "id": "id:a4ayc_80_OEAAAAAAAAAXw",
    //    "client_modified": "2015-05-12T15:50:38Z",
    //    "server_modified": "2015-05-12T15:50:38Z",
    //    "rev": "a1c10ce0dd78",
    //    "size": 7212,
    //    "path_lower": "/homework/math/prime_numbers.txt",
    //    "path_display": "/Homework/math/Prime_Numbers.txt",
    //    "sharing_info": {
    //        "read_only": true,
    //        "parent_shared_folder_id": "84528192421",
    //        "modified_by": "dbid:AAH4f99T0taONIb-OurWxbNQ6ywGRopQngc"
    //    },
    //    "is_downloadable": true,
    //    "property_groups": [
    //        {
    //            "template_id": "ptid:1a5n2i6d3OYEAAAAAAAAAYa",
    //            "fields": [
    //                {
    //                    "name": "Security Policy",
    //                    "value": "Confidential"
    //                }
    //            ]
    //        }
    //    ],
    //    "has_explicit_shared_members": false,
    //    "content_hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    //    "file_lock_info": {
    //        "is_lockholder": true,
    //        "lockholder_name": "Imaginary User",
    //        "created": "2015-05-12T15:50:38Z"
    //    }
    //}


    @SuppressWarnings("unused")
    public static class Field {

        @SerializedName("name")
        @Expose
        private String name;
        @SerializedName("value")
        @Expose
        private String value;

        public Field() {
        }

        public Field(String name, String value) {
            super();
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }


    @SuppressWarnings("unused")
    public static class FileLockInfo {

        @SerializedName("is_lockholder")
        @Expose
        private Boolean isLockholder;
        @SerializedName("lockholder_name")
        @Expose
        private String lockholderName;
        @SerializedName("created")
        @Expose
        private String created;

        public FileLockInfo() {
        }

        public FileLockInfo(Boolean isLockholder, String lockholderName, String created) {
            super();
            this.isLockholder = isLockholder;
            this.lockholderName = lockholderName;
            this.created = created;
        }

        public Boolean getIsLockholder() {
            return isLockholder;
        }

        public void setIsLockholder(Boolean isLockholder) {
            this.isLockholder = isLockholder;
        }

        public String getLockholderName() {
            return lockholderName;
        }

        public void setLockholderName(String lockholderName) {
            this.lockholderName = lockholderName;
        }

        public String getCreated() {
            return created;
        }

        public void setCreated(String created) {
            this.created = created;
        }

    }


    @SuppressWarnings("unused")
    public class FileMetadata {

        @SerializedName(".tag")
        @Expose
        private String tag;
        @SerializedName("name")
        @Expose
        private String name;
        @SerializedName("id")
        @Expose
        private String id;
        @SerializedName("client_modified")
        @Expose
        private String clientModified;
        @SerializedName("server_modified")
        @Expose
        private String serverModified;
        @SerializedName("rev")
        @Expose
        private String rev;
        @SerializedName("size")
        @Expose
        private Integer size;
        @SerializedName("path_lower")
        @Expose
        private String pathLower;
        @SerializedName("path_display")
        @Expose
        private String pathDisplay;
        @SerializedName("sharing_info")
        @Expose
        private SharingInfo sharingInfo;
        @SerializedName("is_downloadable")
        @Expose
        private Boolean isDownloadable;
        @SerializedName("property_groups")
        @Expose
        private List<PropertyGroup> propertyGroups = null;
        @SerializedName("has_explicit_shared_members")
        @Expose
        private Boolean hasExplicitSharedMembers;
        @SerializedName("content_hash")
        @Expose
        private String contentHash;
        @SerializedName("file_lock_info")
        @Expose
        private FileLockInfo fileLockInfo;

        /**
         * No args constructor for use in serialization
         *
         */
        public FileMetadata() {
        }

        /**
         *
         * @param pathDisplay
         * @param fileLockInfo
         * @param rev
         * @param clientModified
         * @param pathLower
         * @param propertyGroups
         * @param contentHash
         * @param isDownloadable
         * @param size
         * @param name
         * @param hasExplicitSharedMembers
         * @param serverModified
         * @param tag
         * @param id
         * @param sharingInfo
         */
        public FileMetadata(String tag, String name, String id, String clientModified, String serverModified, String rev, Integer size, String pathLower, String pathDisplay, SharingInfo sharingInfo, Boolean isDownloadable, List<PropertyGroup> propertyGroups, Boolean hasExplicitSharedMembers, String contentHash, FileLockInfo fileLockInfo) {
            super();
            this.tag = tag;
            this.name = name;
            this.id = id;
            this.clientModified = clientModified;
            this.serverModified = serverModified;
            this.rev = rev;
            this.size = size;
            this.pathLower = pathLower;
            this.pathDisplay = pathDisplay;
            this.sharingInfo = sharingInfo;
            this.isDownloadable = isDownloadable;
            this.propertyGroups = propertyGroups;
            this.hasExplicitSharedMembers = hasExplicitSharedMembers;
            this.contentHash = contentHash;
            this.fileLockInfo = fileLockInfo;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getClientModified() {
            return clientModified;
        }

        public void setClientModified(String clientModified) {
            this.clientModified = clientModified;
        }

        public String getServerModified() {
            return serverModified;
        }

        public void setServerModified(String serverModified) {
            this.serverModified = serverModified;
        }

        public String getRev() {
            return rev;
        }

        public void setRev(String rev) {
            this.rev = rev;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public String getPathLower() {
            return pathLower;
        }

        public void setPathLower(String pathLower) {
            this.pathLower = pathLower;
        }

        public String getPathDisplay() {
            return pathDisplay;
        }

        public void setPathDisplay(String pathDisplay) {
            this.pathDisplay = pathDisplay;
        }

        public SharingInfo getSharingInfo() {
            return sharingInfo;
        }

        public void setSharingInfo(SharingInfo sharingInfo) {
            this.sharingInfo = sharingInfo;
        }

        public Boolean getIsDownloadable() {
            return isDownloadable;
        }

        public void setIsDownloadable(Boolean isDownloadable) {
            this.isDownloadable = isDownloadable;
        }

        public List<PropertyGroup> getPropertyGroups() {
            return propertyGroups;
        }

        public void setPropertyGroups(List<PropertyGroup> propertyGroups) {
            this.propertyGroups = propertyGroups;
        }

        public Boolean getHasExplicitSharedMembers() {
            return hasExplicitSharedMembers;
        }

        public void setHasExplicitSharedMembers(Boolean hasExplicitSharedMembers) {
            this.hasExplicitSharedMembers = hasExplicitSharedMembers;
        }

        public String getContentHash() {
            return contentHash;
        }

        public void setContentHash(String contentHash) {
            this.contentHash = contentHash;
        }

        public FileLockInfo getFileLockInfo() {
            return fileLockInfo;
        }

        public void setFileLockInfo(FileLockInfo fileLockInfo) {
            this.fileLockInfo = fileLockInfo;
        }

    }

    @SuppressWarnings("unused")
    public static class PropertyGroup {

        @SerializedName("template_id")
        @Expose
        private String templateId;
        @SerializedName("fields")
        @Expose
        private List<Field> fields = null;

        public PropertyGroup() {
        }

        public PropertyGroup(String templateId, List<Field> fields) {
            super();
            this.templateId = templateId;
            this.fields = fields;
        }

        public String getTemplateId() {
            return templateId;
        }

        public void setTemplateId(String templateId) {
            this.templateId = templateId;
        }

        public List<Field> getFields() {
            return fields;
        }

        public void setFields(List<Field> fields) {
            this.fields = fields;
        }

    }


    @SuppressWarnings("unused")
    public static class SharingInfo {

        @SerializedName("read_only")
        @Expose
        private Boolean readOnly;
        @SerializedName("parent_shared_folder_id")
        @Expose
        private String parentSharedFolderId;
        @SerializedName("modified_by")
        @Expose
        private String modifiedBy;

        public SharingInfo() {
        }

        public SharingInfo(Boolean readOnly, String parentSharedFolderId, String modifiedBy) {
            super();
            this.readOnly = readOnly;
            this.parentSharedFolderId = parentSharedFolderId;
            this.modifiedBy = modifiedBy;
        }

        public Boolean getReadOnly() {
            return readOnly;
        }

        public void setReadOnly(Boolean readOnly) {
            this.readOnly = readOnly;
        }

        public String getParentSharedFolderId() {
            return parentSharedFolderId;
        }

        public void setParentSharedFolderId(String parentSharedFolderId) {
            this.parentSharedFolderId = parentSharedFolderId;
        }

        public String getModifiedBy() {
            return modifiedBy;
        }

        public void setModifiedBy(String modifiedBy) {
            this.modifiedBy = modifiedBy;
        }

    }


    // SharedLinkMetadata example:
    //
    // {
    //    ".tag": "file",
    //    "url": "https://www.dropbox.com/s/2sn712vy1ovegw8/Prime_Numbers.txt?dl=0",
    //    "name": "Prime_Numbers.txt",
    //    "link_permissions": {
    //        "can_revoke": false,
    //        "resolved_visibility": {
    //            ".tag": "public"
    //        },
    //        "revoke_failure_reason": {
    //            ".tag": "owner_only"
    //        }
    //    },
    //    "client_modified": "2015-05-12T15:50:38Z",
    //    "server_modified": "2015-05-12T15:50:38Z",
    //    "rev": "a1c10ce0dd78",
    //    "size": 7212,
    //    "id": "id:a4ayc_80_OEAAAAAAAAAXw",
    //    "path_lower": "/homework/math/prime_numbers.txt",
    //    "team_member_info": {
    //        "team_info": {
    //            "id": "dbtid:AAFdgehTzw7WlXhZJsbGCLePe8RvQGYDr-I",
    //            "name": "Acme, Inc."
    //        },
    //        "display_name": "Roger Rabbit",
    //        "member_id": "dbmid:abcd1234"
    //    }
    //}

    @SuppressWarnings("unused")
    public static class LinkPermissions {

        @SerializedName("can_revoke")
        @Expose
        private Boolean canRevoke;
        @SerializedName("resolved_visibility")
        @Expose
        private ResolvedVisibility resolvedVisibility;
        @SerializedName("revoke_failure_reason")
        @Expose
        private RevokeFailureReason revokeFailureReason;

        public LinkPermissions() {
        }

        public LinkPermissions(Boolean canRevoke, ResolvedVisibility resolvedVisibility, RevokeFailureReason revokeFailureReason) {
            super();
            this.canRevoke = canRevoke;
            this.resolvedVisibility = resolvedVisibility;
            this.revokeFailureReason = revokeFailureReason;
        }

        public Boolean getCanRevoke() {
            return canRevoke;
        }

        public void setCanRevoke(Boolean canRevoke) {
            this.canRevoke = canRevoke;
        }

        public ResolvedVisibility getResolvedVisibility() {
            return resolvedVisibility;
        }

        public void setResolvedVisibility(ResolvedVisibility resolvedVisibility) {
            this.resolvedVisibility = resolvedVisibility;
        }

        public RevokeFailureReason getRevokeFailureReason() {
            return revokeFailureReason;
        }

        public void setRevokeFailureReason(RevokeFailureReason revokeFailureReason) {
            this.revokeFailureReason = revokeFailureReason;
        }

    }


    @SuppressWarnings("unused")
    public static class ResolvedVisibility {

        @SerializedName(".tag")
        @Expose
        private String tag;

        public ResolvedVisibility() {
        }

        public ResolvedVisibility(String tag) {
            super();
            this.tag = tag;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

    }


    @SuppressWarnings("unused")
    public static class RevokeFailureReason {

        @SerializedName(".tag")
        @Expose
        private String tag;

        public RevokeFailureReason() {
        }

        public RevokeFailureReason(String tag) {
            super();
            this.tag = tag;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

    }


    @SuppressWarnings("unused")
    public static class SharedLinkMetadata {

        @SerializedName(".tag")
        @Expose
        private String tag;
        @SerializedName("url")
        @Expose
        private String url;
        @SerializedName("name")
        @Expose
        private String name;
        @SerializedName("link_permissions")
        @Expose
        private LinkPermissions linkPermissions;
        @SerializedName("client_modified")
        @Expose
        private String clientModified;
        @SerializedName("server_modified")
        @Expose
        private String serverModified;
        @SerializedName("rev")
        @Expose
        private String rev;
        @SerializedName("size")
        @Expose
        private Integer size;
        @SerializedName("id")
        @Expose
        private String id;
        @SerializedName("path_lower")
        @Expose
        private String pathLower;
        @SerializedName("team_member_info")
        @Expose
        private TeamMemberInfo teamMemberInfo;

        public SharedLinkMetadata() {
        }

        public SharedLinkMetadata(String tag, String url, String name, LinkPermissions linkPermissions, String clientModified, String serverModified, String rev, Integer size, String id, String pathLower, TeamMemberInfo teamMemberInfo) {
            super();
            this.tag = tag;
            this.url = url;
            this.name = name;
            this.linkPermissions = linkPermissions;
            this.clientModified = clientModified;
            this.serverModified = serverModified;
            this.rev = rev;
            this.size = size;
            this.id = id;
            this.pathLower = pathLower;
            this.teamMemberInfo = teamMemberInfo;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public LinkPermissions getLinkPermissions() {
            return linkPermissions;
        }

        public void setLinkPermissions(LinkPermissions linkPermissions) {
            this.linkPermissions = linkPermissions;
        }

        public String getClientModified() {
            return clientModified;
        }

        public void setClientModified(String clientModified) {
            this.clientModified = clientModified;
        }

        public String getServerModified() {
            return serverModified;
        }

        public void setServerModified(String serverModified) {
            this.serverModified = serverModified;
        }

        public String getRev() {
            return rev;
        }

        public void setRev(String rev) {
            this.rev = rev;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPathLower() {
            return pathLower;
        }

        public void setPathLower(String pathLower) {
            this.pathLower = pathLower;
        }

        public TeamMemberInfo getTeamMemberInfo() {
            return teamMemberInfo;
        }

        public void setTeamMemberInfo(TeamMemberInfo teamMemberInfo) {
            this.teamMemberInfo = teamMemberInfo;
        }

    }

    @SuppressWarnings("unused")
    public static class TeamInfo {

        @SerializedName("id")
        @Expose
        private String id;
        @SerializedName("name")
        @Expose
        private String name;

        public TeamInfo() {
        }

        public TeamInfo(String id, String name) {
            super();
            this.id = id;
            this.name = name;
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

    }

    @SuppressWarnings("unused")
    public static class TeamMemberInfo {

        @SerializedName("team_info")
        @Expose
        private TeamInfo teamInfo;
        @SerializedName("display_name")
        @Expose
        private String displayName;
        @SerializedName("member_id")
        @Expose
        private String memberId;

        public TeamMemberInfo() {
        }

        public TeamMemberInfo(TeamInfo teamInfo, String displayName, String memberId) {
            super();
            this.teamInfo = teamInfo;
            this.displayName = displayName;
            this.memberId = memberId;
        }

        public TeamInfo getTeamInfo() {
            return teamInfo;
        }

        public void setTeamInfo(TeamInfo teamInfo) {
            this.teamInfo = teamInfo;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getMemberId() {
            return memberId;
        }

        public void setMemberId(String memberId) {
            this.memberId = memberId;
        }

    }

}
