package com.unsubscribeos.core.model;

import java.util.List;

public record OAuthConfig(
        String clientId,
        String clientSecret,
        String authEndpoint,
        String tokenEndpoint,
        List<String> scopes) {

    public boolean hasSecret() {
        return clientSecret != null && !clientSecret.isBlank();
    }

    public String scopeString() {
        return String.join(" ", scopes);
    }
}
