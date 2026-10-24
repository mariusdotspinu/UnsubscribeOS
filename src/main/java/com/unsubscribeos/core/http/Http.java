package com.unsubscribeos.core.http;

import com.fasterxml.jackson.databind.JsonNode;

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

public final class Http {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .build();

    private Http() {}

    public static String get(String url, String accessToken) {
        return send(bearer(url, accessToken).GET().build());
    }

    public static String postForm(String url, Map<String, String> form) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(query(form)))
                .build();
        return send(request);
    }

    public static String postJson(String url, String accessToken, String json) {
        return send(bearer(url, accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build());
    }

    public static int postRaw(String url, String contentType, String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", contentType)
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            return CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (Exception e) {
            throw new HttpException("Couldn't reach " + URI.create(url).getHost(), e);
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
            String reason = reason(response.body());
            throw new HttpException("HTTP " + code + (reason.isEmpty() ? "" : ": " + reason), code);
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            throw new HttpException("Couldn't reach " + request.uri().getHost(), e);
        }
    }

    // Google/Graph nest {"error":{"message"}}; OAuth token errors use error_description or a bare error.
    static String reason(String body) {
        try {
            JsonNode json = Json.parse(body), error = json.path("error");
            String text = json.path("error_description").asText(error.path("message").asText(error.asText("")));
            return text.split("(?<=\\.)\\s", 2)[0].trim();
        } catch (RuntimeException notJson) {
            return "";
        }
    }

    public static String query(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    public static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
