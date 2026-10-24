package com.unsubscribeos.core.model;

public record Account(Provider provider, String clientId, String clientSecret, TokenSet tokens) {

    public Account withTokens(TokenSet updated) {
        return new Account(provider, clientId, clientSecret, updated);
    }
}
