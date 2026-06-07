package com.unsubscribeos.core.model;

/**
 * A signed-in account: the provider, the user-supplied client credentials (needed to
 * silently refresh on the next launch) and the current tokens. Persisted encrypted so
 * the user stays logged in until they explicitly sign out.
 */
public record Account(Provider provider, String clientId, String clientSecret, TokenSet tokens) {

    public Account withTokens(TokenSet updated) {
        return new Account(provider, clientId, clientSecret, updated);
    }
}
