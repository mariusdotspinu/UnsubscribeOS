package com.unsubscribeos.core.mail.gmail;

import com.fasterxml.jackson.databind.JsonNode;
import com.unsubscribeos.core.http.Http;
import com.unsubscribeos.core.http.Json;
import com.unsubscribeos.core.mail.FetchContext;
import com.unsubscribeos.core.mail.MailService;
import com.unsubscribeos.core.model.EmailMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gmail REST v1 provider. Lists recent received-mail ids (paged), fetches metadata for each
 * concurrently, and trashes via {@code batchModify} (adding the TRASH label) — permitted under
 * the {@code gmail.modify} scope and recoverable. The fetch/delete skeleton lives in
 * {@link MailService}; this class supplies only the Gmail specifics.
 */
public final class GmailService extends MailService {

    private static final String BASE = "https://gmail.googleapis.com/gmail/v1/users/me/messages";
    // Every message anyone sent us: spam/trash are excluded by default, and we skip our own
    // sent/chat mail, which would otherwise group under our own domain.
    private static final String QUERY = "-in:sent -in:chats";
    private static final String METADATA = "?format=metadata" + List.of("From", "Subject", "Date",
            "List-Unsubscribe", "List-Unsubscribe-Post").stream()
            .map(h -> "&metadataHeaders=" + Http.enc(h)).collect(Collectors.joining());

    public GmailService() {
        super(1000);
    }

    @Override
    protected void deleteBatch(String accessToken, List<String> messageIds) {
        String body = Json.write(Map.of("ids", messageIds, "addLabelIds", List.of("TRASH")));
        Http.postJson(BASE + "/batchModify", accessToken, body);
    }

    @Override
    protected List<String> listMessageIds(String accessToken, int maxMessages, FetchContext ctx) {
        List<String> ids = new ArrayList<>();
        String pageToken = null;
        do {
            if (ctx.isCancelled()) break;
            String url = BASE + "?maxResults=500&q=" + Http.enc(QUERY)
                    + (pageToken == null ? "" : "&pageToken=" + Http.enc(pageToken));
            JsonNode page = Json.parse(Http.get(url, accessToken));
            page.path("messages").forEach(m -> ids.add(m.path("id").asText()));
            pageToken = page.path("nextPageToken").asText(null);
        } while (pageToken != null && ids.size() < maxMessages);
        return ids.size() > maxMessages ? ids.subList(0, maxMessages) : ids;
    }

    @Override
    protected EmailMessage fetchMessage(String accessToken, String id) {
        JsonNode node = Json.parse(Http.get(BASE + "/" + id + METADATA, accessToken));
        Map<String, String> headers = headers(node.path("payload").path("headers"));
        return message(id, headers.get("from"), headers.getOrDefault("subject", "(no subject)"),
                node.path("snippet").asText(""), Instant.ofEpochMilli(node.path("internalDate").asLong(0)), headers);
    }
}
