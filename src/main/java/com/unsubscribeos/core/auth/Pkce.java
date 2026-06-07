package com.unsubscribeos.core.auth;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * RFC 7636 PKCE pair. The verifier is a high-entropy secret kept in memory; the S256
 * challenge is what travels in the authorization URL, so an intercepted code is useless.
 */
public record Pkce(String verifier, String challenge) {

    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder URL = Base64.getUrlEncoder().withoutPadding();

    public static Pkce create() {
        byte[] raw = new byte[32];
        RNG.nextBytes(raw);
        String verifier = URL.encodeToString(raw);
        return new Pkce(verifier, challenge(verifier));
    }

    private static String challenge(String verifier) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return URL.encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
