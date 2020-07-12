package info.ginj.export.online;

import info.ginj.model.Account;

import java.util.Date;
import java.util.List;

public class OAuthAccount extends Account {
    private String accessToken;

    // Note : accessExpiry is a Date instead of LocalDateTime because the latter is not a Java Bean and cannot be serialized using XMLEncoder
    private Date accessExpiry;
    private String refreshToken;
    private List<String> allowedScopes;


    public OAuthAccount() {
        super();
    }

    public OAuthAccount(String id, String name, String email, String accessToken, Date accessExpiry, String refreshToken, List<String> allowedScopes) {
        super(id, name, email);
        this.accessToken = accessToken;
        this.accessExpiry = accessExpiry;
        this.refreshToken = refreshToken;
        this.allowedScopes = allowedScopes;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Date getAccessExpiry() {
        return accessExpiry;
    }

    public void setAccessExpiry(Date accessExpiry) {
        this.accessExpiry = accessExpiry;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public List<String> getAllowedScopes() {
        return allowedScopes;
    }

    public void setAllowedScopes(List<String> allowedScopes) {
        this.allowedScopes = allowedScopes;
    }
}
