package com.unsubscribeos.core.model;

import java.time.Instant;

/**
 * OAuth tokens for a signed-in account. Persisted (encrypted) so the user stays
 * logged in across restarts. {@code refreshToken} may be null if the provider did
 * not return one on a silent refresh.
 */
public record TokenSet(String accessToken, String refreshToken, Instant expiresAt) {

    /** True once we are within a minute of expiry, so callers refresh pre-emptively. */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt.minusSeconds(60));
    }

    /** Returns a copy carrying forward the previous refresh token when a refresh omits one. */
    public TokenSet mergeRefreshToken(TokenSet previous) {
        return refreshToken != null ? this
                : new TokenSet(accessToken, previous.refreshToken(), expiresAt);
    }
}
