package info.ginj.export.online;

import info.ginj.model.Account;

import java.time.LocalDateTime;
import java.util.List;

public class OAuthAccount extends Account {
    String accessToken;
    LocalDateTime accessExpiry;
    String refreshToken;
    List<String> allowedScopes;


    public OAuthAccount(String id, String name, String email, String accessToken, LocalDateTime accessExpiry, String refreshToken, List<String> allowedScopes) {
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

    public LocalDateTime getAccessExpiry() {
        return accessExpiry;
    }

    public void setAccessExpiry(LocalDateTime accessExpiry) {
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
