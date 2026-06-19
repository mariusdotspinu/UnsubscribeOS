package com.unsubscribeos.core.mail.gmail;

import com.fasterxml.jackson.databind.JsonNode;
import com.unsubscribeos.core.http.Http;
import com.unsubscribeos.core.http.Json;
import com.unsubscribeos.core.mail.AbstractMailService;
import com.unsubscribeos.core.mail.Addresses;
import com.unsubscribeos.core.mail.BulkMail;
import com.unsubscribeos.core.mail.FetchContext;
import com.unsubscribeos.core.mail.UnsubscribeHeaders;
import com.unsubscribeos.core.model.EmailMessage;
import com.unsubscribeos.core.model.Provider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gmail REST v1 provider. Lists recent bulk-mail ids (paged), fetches metadata for each
 * concurrently, and trashes via {@code batchModify} (adding the TRASH label) — permitted under
 * the {@code gmail.modify} scope and recoverable. The fetch/delete skeleton lives in
 * {@link AbstractMailService}; this class supplies only the Gmail specifics.
 */
public final class GmailService extends AbstractMailService {

    private static final String BASE = "https://gmail.googleapis.com/gmail/v1/users/me/messages";
    // Scan all received mail (spam/trash are excluded by default; skip your own sent/chat mail).
    // We no longer pre-filter to "promotions/unsubscribe" here — the dashboard keeps only senders
    // that actually expose a List-Unsubscribe header, which catches bulk mail Gmail filed elsewhere.
    private static final String QUERY = "-in:sent -in:chats";
    private static final int BATCH_LIMIT = 1000;
    private static final List<String> HEADERS = List.of("From", "Subject", "Date",
            "List-Unsubscribe", "List-Unsubscribe-Post", "List-Id", "List-Post", "Precedence", "Auto-Submitted");

    @Override
    public Provider provider() {
        return Provider.GMAIL;
    }

    @Override
    protected int batchLimit() {
        return BATCH_LIMIT;
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
    protected Optional<EmailMessage> fetchMessage(String accessToken, String id) {
        String url = BASE + "/" + id + "?format=metadata"
                + HEADERS.stream().map(h -> "&metadataHeaders=" + Http.enc(h)).reduce("", String::concat);
        return Optional.of(toMessage(id, Json.parse(Http.get(url, accessToken))));
    }

    private EmailMessage toMessage(String id, JsonNode node) {
        Map<String, String> headers = headerMap(node.path("payload").path("headers"));
        Addresses.Sender sender = Addresses.parseFrom(headers.get("from"));
        return new EmailMessage(
                id,
                sender.name(),
                sender.address(),
                sender.domain(),
                headers.getOrDefault("subject", "(no subject)"),
                node.path("snippet").asText(""),
                Instant.ofEpochMilli(node.path("internalDate").asLong(0)),
                UnsubscribeHeaders.parse(headers.get("list-unsubscribe"), headers.get("list-unsubscribe-post"), sender.domain()),
                BulkMail.isBulk(headers));
    }

    private Map<String, String> headerMap(JsonNode headers) {
        Map<String, String> map = new HashMap<>();
        headers.forEach(h -> map.put(h.path("name").asText().toLowerCase(), h.path("value").asText()));
        return map;
    }
}
