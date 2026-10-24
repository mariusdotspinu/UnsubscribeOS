package com.unsubscribeos.core.model;

import java.time.Instant;

public record TokenSet(String accessToken, String refreshToken, Instant expiresAt) {

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt.minusSeconds(60));
    }

    public TokenSet mergeRefreshToken(TokenSet previous) {
        return refreshToken != null ? this
                : new TokenSet(accessToken, previous.refreshToken(), expiresAt);
    }
}
