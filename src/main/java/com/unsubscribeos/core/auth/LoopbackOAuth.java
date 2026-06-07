package com.unsubscribeos.core.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import com.unsubscribeos.core.http.Http;
import com.unsubscribeos.core.http.Json;
import com.unsubscribeos.core.model.OAuthConfig;
import com.unsubscribeos.core.model.TokenSet;
import com.unsubscribeos.core.platform.Browser;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * OAuth 2.0 Authorization Code flow with PKCE over a 127.0.0.1 loopback redirect — the
 * recommended pattern for native desktop apps. No embedded browser, no listening on a
 * public interface, and the authorization code is bound to a one-time PKCE verifier.
 */
public final class LoopbackOAuth {

    private static final SecureRandom RNG = new SecureRandom();
    private static final int AUTH_TIMEOUT_MINUTES = 5;

    private final OAuthConfig config;

    public LoopbackOAuth(OAuthConfig config) {
        this.config = config;
    }

    /** Runs the full interactive sign-in and returns the resulting tokens. Blocking. */
    public TokenSet authorize() {
        Pkce pkce = Pkce.create();
        String state = randomState();
        HttpServer server = startServer();
        try {
            String redirectUri = "http://localhost:" + server.getAddress().getPort() + "/callback";
            CompletableFuture<String> code = new CompletableFuture<>();
            server.createContext("/callback", exchange -> {
                Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
                respond(exchange, landingPage(params));
                if (state.equals(params.get("state")) && params.containsKey("code")) {
                    code.complete(params.get("code"));
                } else {
                    code.completeExceptionally(new IllegalStateException(
                            params.getOrDefault("error", "Authorization was cancelled or state mismatched")));
                }
            });
            Browser.open(authUrl(redirectUri, state, pkce));
            String authCode = await(code);
            return exchangeCode(authCode, redirectUri, pkce);
        } finally {
            server.stop(0);
        }
    }

    /** Exchanges a stored refresh token for a fresh access token. */
    public TokenSet refresh(String refreshToken) {
        Map<String, String> form = baseForm();
        form.put("grant_type", "refresh_token");
        form.put("refresh_token", refreshToken);
        if (!config.scopes().isEmpty()) form.put("scope", config.scopeString());
        return parseTokens(Json.parse(Http.postForm(config.tokenEndpoint(), form)));
    }

    // ---- internals ----------------------------------------------------------

    private String authUrl(String redirectUri, String state, Pkce pkce) {
        Map<String, String> q = new HashMap<>();
        q.put("client_id", config.clientId());
        q.put("redirect_uri", redirectUri);
        q.put("response_type", "code");
        q.put("scope", config.scopeString());
        q.put("state", state);
        q.put("code_challenge", pkce.challenge());
        q.put("code_challenge_method", "S256");
        q.put("access_type", "offline");   // Google: ensures a refresh token
        q.put("prompt", "consent");        // force refresh-token issuance on re-consent
        String query = q.entrySet().stream()
                .map(e -> Http.enc(e.getKey()) + "=" + Http.enc(e.getValue()))
                .reduce((a, b) -> a + "&" + b).orElseThrow();
        return config.authEndpoint() + "?" + query;
    }

    private TokenSet exchangeCode(String code, String redirectUri, Pkce pkce) {
        Map<String, String> form = baseForm();
        form.put("grant_type", "authorization_code");
        form.put("code", code);
        form.put("redirect_uri", redirectUri);
        form.put("code_verifier", pkce.verifier());
        return parseTokens(Json.parse(Http.postForm(config.tokenEndpoint(), form)));
    }

    private Map<String, String> baseForm() {
        Map<String, String> form = new HashMap<>();
        form.put("client_id", config.clientId());
        if (config.hasSecret()) form.put("client_secret", config.clientSecret());
        return form;
    }

    private TokenSet parseTokens(JsonNode node) {
        if (node.has("error")) {
            throw new IllegalStateException("Token endpoint error: " + node.path("error_description").asText(node.path("error").asText()));
        }
        String access = node.path("access_token").asText(null);
        String refresh = node.path("refresh_token").asText(null);
        long expiresIn = node.path("expires_in").asLong(3600);
        return new TokenSet(access, refresh, Instant.now().plusSeconds(expiresIn));
    }

    private HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.start();
            return server;
        } catch (Exception e) {
            throw new IllegalStateException("Could not start local callback server", e);
        }
    }

    private static String await(CompletableFuture<String> code) {
        try {
            return code.get(AUTH_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Sign-in timed out", e);
        } catch (Exception e) {
            throw new IllegalStateException(e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e);
        }
    }

    private static String randomState() {
        byte[] b = new byte[16];
        RNG.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> out = new HashMap<>();
        if (raw == null) return out;
        for (String pair : raw.split("&")) {
            int i = pair.indexOf('=');
            if (i > 0) out.put(urlDecode(pair.substring(0, i)), urlDecode(pair.substring(i + 1)));
        }
        return out;
    }

    private static String urlDecode(String s) {
        return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static String landingPage(Map<String, String> params) {
        boolean ok = params.containsKey("code");
        String title = ok ? "You're signed in ✓" : "Sign-in failed";
        String body = ok ? "You can close this tab and return to UnsubscribeOS."
                         : "Reason: " + params.getOrDefault("error", "unknown") + ". You can close this tab.";
        return """
                <!doctype html><html><head><meta charset="utf-8"><title>UnsubscribeOS</title></head>
                <body style="font-family:system-ui;background:#0d1117;color:#e6edf3;display:flex;
                height:100vh;margin:0;align-items:center;justify-content:center;text-align:center">
                <div><h2 style="color:#4f8cff">%s</h2><p>%s</p></div></body></html>"""
                .formatted(title, body);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String html) {
        try (OutputStream os = exchange.getResponseBody()) {
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            os.write(bytes);
        } catch (Exception ignored) {
            // client closed; nothing actionable
        }
    }
}
