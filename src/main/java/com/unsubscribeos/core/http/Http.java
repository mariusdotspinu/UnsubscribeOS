package com.unsubscribeos.core.http;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Small wrapper over the JDK {@link HttpClient}. Uses a virtual-thread executor so the
 * many concurrent message fetches stay cheap, and centralises bearer auth, form encoding
 * and error mapping for every provider.
 */
public final class Http {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .build();

    private Http() {}

    public static String get(String url, String accessToken) {
        return send(bearer(url, accessToken).GET().build());
    }

    public static String get(String url, String accessToken, Map<String, String> headers) {
        HttpRequest.Builder builder = bearer(url, accessToken);
        headers.forEach(builder::header);
        return send(builder.GET().build());
    }

    public static String postForm(String url, Map<String, String> form) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formEncode(form)))
                .build();
        return send(request);
    }

    public static String postJson(String url, String accessToken, String json) {
        return send(bearer(url, accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build());
    }

    public static void delete(String url, String accessToken) {
        send(bearer(url, accessToken).DELETE().build());
    }

    /** Fire-and-forget HTTPS POST used for RFC 8058 one-click unsubscribe; returns status code. */
    public static int postRaw(String url, String contentType, String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", contentType)
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            return CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (Exception e) {
            throw new HttpException("POST " + url + " failed: " + e.getMessage(), e);
        }
    }

    private static HttpRequest.Builder bearer(String url, String accessToken) {
        return HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .timeout(Duration.ofSeconds(30));
    }

    private static String send(HttpRequest request) {
        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code >= 200 && code < 300) return response.body();
            throw new HttpException("HTTP " + code + " for " + request.uri() + ": " + response.body());
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            throw new HttpException("Request failed: " + request.uri() + " — " + e.getMessage(), e);
        }
    }

    private static String formEncode(Map<String, String> form) {
        return form.entrySet().stream()
                .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    public static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
