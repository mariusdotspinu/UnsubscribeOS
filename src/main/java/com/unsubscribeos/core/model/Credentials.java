package com.unsubscribeos.core.model;

/**
 * A provider's user-supplied OAuth client credentials, remembered (encrypted) across sign-outs
 * when "Remember me" is chosen, so the fields can be pre-filled on the next sign-in.
 * {@code clientSecret} may be null for providers that don't require one.
 */
public record Credentials(String clientId, String clientSecret) {
}
