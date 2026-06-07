package com.unsubscribeos.core.model;

import java.util.List;

/**
 * Endpoints and scopes for an OAuth 2.0 provider, combined with the user-supplied
 * client credentials. The client secret is optional (public PKCE clients omit it).
 */
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
