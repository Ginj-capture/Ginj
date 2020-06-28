package info.ginj.export.online.dropbox;

import com.dropbox.core.*;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;
import com.dropbox.core.v2.sharing.SharedLinkSettings;
import com.dropbox.core.v2.users.FullAccount;
import info.ginj.Capture;
import info.ginj.Ginj;
import info.ginj.Prefs;
import info.ginj.export.online.AbstractOnlineExporter;
import info.ginj.export.online.exception.AuthorizationException;
import info.ginj.export.online.exception.CommunicationException;
import info.ginj.export.online.exception.UploadException;
import info.ginj.ui.Util;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;


public class DropboxExporter extends AbstractOnlineExporter {
    private static final String DROPBOX_CLIENT_APP_KEY = "pdio3i9brehyjo1";
    private static final String DROPBOX_OAUTH2_AUTH_URL = "https://www.dropbox.com/oauth2/authorize";
    private static final String DROPBOX_OAUTH2_TOKEN_URL = "https://api.dropboxapi.com/oauth2/token";
    private static final String DROPBOX_REVOKE_URL = "https://www.dropbox.com/account/connected_apps";
    private DbxPKCEWebAuth pkceWebAuth;


    public DropboxExporter(JFrame frame) {
        super(frame);
    }


    @Override
    public String getServiceName() {
        return "Dropbox";
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
    protected String getOAuth2AuthUrl() {
        return DROPBOX_OAUTH2_AUTH_URL;
    }

    @Override
    protected String getOAuth2TokenUrl() {
        return DROPBOX_OAUTH2_TOKEN_URL;
    }

    @Override
    protected String getOAuth2RevokeUrl() {
        return DROPBOX_REVOKE_URL;
    }

    protected Prefs.Key getRefreshTokenKeyPrefix() {
        return Prefs.Key.EXPORTER_DROPBOX_REFRESH_TOKEN_PREFIX;
    }

    protected Prefs.Key getAccessTokenKeyPrefix() {
        return Prefs.Key.EXPORTER_DROPBOX_ACCESS_TOKEN_PREFIX;
    }

    protected Prefs.Key getAccessExpiryKeyPrefix() {
        return Prefs.Key.EXPORTER_DROPBOX_ACCESS_EXPIRY_PREFIX;
    }

    @Override
    protected String[] getRequiredScopes() {
        // For Dropbox, no scope means permissions defined at the app level on the site
        return null;
    }


    /**
     * Exports the given capture
     * This method is run in its own thread and should not access the GUI directly. All interaction
     * should go through synchronized objects or be enclosed in a SwingUtilities.invokeLater() logic
     *
     * TODO re-implement using home-made http layer
     *
     * @param capture        the capture to export
     * @param accountNumber  the accountNumber to export this capture to
     */
    @Override
    public void exportCapture(Capture capture, String accountNumber) {
        try {
            final String captureUrl = uploadCapture(capture, accountNumber);
            if (captureUrl != null) {
                copyTextToClipboard(captureUrl);
                // Indicate export is complete.
                complete("Upload successful. A link to your capture was copied to the clipboard");
            }
        }
        catch (Exception e) {
            Util.alertException(getFrame(), getServiceName() + "Error", "There was an error exporting to " + getServiceName(), e);
            failed("Upload error");
        }
    }

    /**
     * Should work, but more work is needed to finish the transaction after the code has been entered.
     * Note: for PKCE, see https://auth0.com/docs/flows/guides/auth-code-pkce/add-login-auth-code-pkce
     *
     * @return the url to call
     */
    private String getAuthenticationUrlByHand() {
        try {
            // Create a Code Verifier
            SecureRandom sr = new SecureRandom();
            byte[] code = new byte[32];
            sr.nextBytes(code);
            final Base64.Encoder encoder = Base64.getUrlEncoder();
            String verifier = encoder.encodeToString(code);

            // Create a Code Challenge
            byte[] bytes = verifier.getBytes(StandardCharsets.US_ASCII);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(bytes, 0, bytes.length);
            byte[] digest = md.digest();
            String challenge = encoder.encodeToString(digest).replaceAll("=+$", ""); // remove trailing equal;

            // Authorize the User
            return getOAuth2AuthUrl()
                    + "?client_id=" + getClientAppId()
                    + "&response_type=code"
                    + "&code_challenge=" + challenge
                    + "&code_challenge_method=S256";
        }
        catch (NoSuchAlgorithmException e) {
            System.err.println("Error authenticating user to Dropbox: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void OLDauthorize(String accountNumber) throws AuthorizationException, CommunicationException {
        // Start authentication procedure
        DbxRequestConfig requestConfig = new DbxRequestConfig("Ginj");
        DbxAppInfo appInfoWithoutSecret = new DbxAppInfo(getClientAppId());
        pkceWebAuth = new DbxPKCEWebAuth(requestConfig, appInfoWithoutSecret);
        DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder()
                .withNoRedirect()
                .withTokenAccessType(TokenAccessType.OFFLINE)
                .build();

        String url = pkceWebAuth.authorize(webAuthRequest);
        receivedCode = JOptionPane.showInputDialog(getFrame(),
                Util.createClickableHtmlEditorPane(
                        "Please visit <a href='" + url + "'>this Dropbox page</a>, <br>allow " + Ginj.getAppName() + " (you may have to log in to Dropbox first), <br>copy the generated authorization key and paste it below."
                ),
                "Dropbox authentication", JOptionPane.QUESTION_MESSAGE
        );
        exchangeCodeForTokens(receivedCode, accountNumber);
    }

    protected void OLDexchangeCodeForTokens(String receivedCode, String accountNumber) throws AuthorizationException, CommunicationException {
        if (receivedCode == null) {
            throw new AuthorizationException("No authorization code was provided");
        }
        receivedCode = receivedCode.trim();
        try { // com.dropbox.core.DbxPKCEManager.makeTokenRequest
            DbxAuthFinish authFinish = pkceWebAuth.finishFromCode(receivedCode);
            String userToken = authFinish.getAccessToken();
            String userName = getUserName(userToken);
            if (userName != null) {
                Prefs.setWithSuffix(Prefs.Key.EXPORTER_DROPBOX_ACCESS_TOKEN_PREFIX, accountNumber, userToken);
                Prefs.setWithSuffix(Prefs.Key.EXPORTER_DROPBOX_USERNAME_PREFIX, accountNumber, userName);
                // Dropbox does not use the following fields for now. Tokens currently never expire.
                // Prefs.setWithSuffix(Prefs.Key.EXPORTER_DROPBOX_REFRESH_TOKEN_PREFIX, accountNumber, authFinish.getRefreshToken());
                // Prefs.setWithSuffix(Prefs.Key.EXPORTER_DROPBOX_EXPIRES_AT_PREFIX, accountNumber, String.valueOf(authFinish.getExpiresAt()));
//                    JOptionPane.showMessageDialog(getFrame(), "You are successfully authenticated to Dropbox as " + userName, "Dropbox authentication", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        catch (DbxException e) {
            throw new CommunicationException("Error authorizing Dropbox user", e);
        }
    }

    /**
     * This method checks that Dropbox authentication is valid
     * @param accountNumber  the accountNumber to export this capture to
     * @throws CommunicationException in case a communication error occurs
     * @throws AuthorizationException in case authorization fails
     */
    @Override
    public void checkAuthorizations(String accountNumber) throws CommunicationException, AuthorizationException {
        logProgress("Checking authorization", 2);
        //String userToken = Prefs.getWithSuffix(Prefs.Key.EXPORTER_DROPBOX_ACCESS_TOKEN_PREFIX, accountNumber);
        String userToken = getAccessToken(accountNumber);
        if (userToken != null) {
            if (getUserName(userToken) == null) {
                throw new AuthorizationException("Received empty username");
            }
        }
    }

    private String getUserName(String userToken) throws CommunicationException, AuthorizationException {
        try {
            // Create Dropbox client
            DbxRequestConfig config = new DbxRequestConfig(Ginj.getAppName() + "/" + Ginj.getVersion());
            DbxClientV2 client = new DbxClientV2(config, userToken);

            // Get current account info
            FullAccount account = client.users().getCurrentAccount();
            return account.getName().getDisplayName();
        }
        catch (InvalidAccessTokenException e) {
            throw new AuthorizationException("Authentication error", e);
        }
        catch (DbxException e) {
            throw new CommunicationException("Error checking authorization", e);
        }
    }

    @Override
    public String uploadCapture(Capture capture, String accountNumber) throws AuthorizationException, UploadException, CommunicationException {
        logProgress("Preparing upload", 10);
        final String targetFileName = "/Applications/" + Ginj.getAppName() + "/" + capture.getDefaultName() + ".png";
        try {
            String accessToken = Prefs.getWithSuffix(Prefs.Key.EXPORTER_DROPBOX_ACCESS_TOKEN_PREFIX, accountNumber);
            DbxRequestConfig config = new DbxRequestConfig(Ginj.getAppName() + "/" + Ginj.getVersion());
            DbxClientV2 client = new DbxClientV2(config, accessToken); // TODO move to exportSettings to support multi-account

            // TODO Upload should be done using sessions - uploadSessionStart etc.
            // Required for big files, but also for progress
            final File fileToUpload = capture.toFile();
            if (fileToUpload.length() < 150_000_000) {
                try (InputStream in = new FileInputStream(fileToUpload)) {
                    logProgress("Uploading file", 50);
                    client.files().uploadBuilder(targetFileName).uploadAndFinish(in);
                }
                if (Prefs.isTrueWithSuffix(Prefs.Key.EXPORTER_DROPBOX_CREATE_LINK_PREFIX, accountNumber)) {
                    final SharedLinkMetadata sharedLinkMetadata = client.sharing().createSharedLinkWithSettings(targetFileName, new SharedLinkSettings());
                    return sharedLinkMetadata.getUrl();
                }
            }
            else {
                throw new UploadException("Upload of big files not implemented yet");
            }
        }
        catch (IOException | DbxException e) {
            throw new UploadException(e);
        }
        return null;
    }

}
