package com.unsubscribeos.core.auth;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public record Pkce(String verifier, String challenge) {

    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder URL = Base64.getUrlEncoder().withoutPadding();

    public static Pkce create() {
        String verifier = random(32);
        return new Pkce(verifier, challenge(verifier));
    }

    static String random(int bytes) {
        byte[] raw = new byte[bytes];
        RNG.nextBytes(raw);
        return URL.encodeToString(raw);
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
