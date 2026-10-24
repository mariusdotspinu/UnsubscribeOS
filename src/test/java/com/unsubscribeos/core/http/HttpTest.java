package com.unsubscribeos.core.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpTest {

    @Test
    void reasonFromProviderErrorBodies() {
        assertEquals("Request had invalid authentication credentials.", Http.reason(
                "{\"error\":{\"code\":401,\"message\":\"Request had invalid authentication credentials. Expected OAuth 2 access token.\"}}"));
        assertEquals("Bad client secret", Http.reason("{\"error\":\"invalid_client\",\"error_description\":\"Bad client secret\"}"));
        assertEquals("invalid_grant", Http.reason("{\"error\":\"invalid_grant\"}"));
        assertEquals("", Http.reason("<html>Bad Gateway</html>"));
    }
}
