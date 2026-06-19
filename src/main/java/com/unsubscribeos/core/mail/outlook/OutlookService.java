package com.unsubscribeos.core.mail.outlook;

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
 * Microsoft Graph provider. Mirrors Gmail: a cheap paged sweep collects the ids of bulk mail
 * (Graph {@code $search="unsubscribe"}), then each message is fetched concurrently; deletion uses
 * Graph {@code $batch} to move messages to Deleted Items (recoverable). The fetch/delete skeleton
 * lives in {@link AbstractMailService}.
 */
public final class OutlookService extends AbstractMailService {

    private static final String ROOT = "https://graph.microsoft.com/v1.0";
    private static final String MESSAGES = ROOT + "/me/messages";
    private static final int PAGE_SIZE = 100;
    private static final int BATCH_LIMIT = 20;
    // List all mail newest-first; the dashboard keeps only senders that expose a List-Unsubscribe
    // header. ($orderby can't be combined with $search, so we drop the keyword filter entirely.)
    private static final String ORDER_BY = "receivedDateTime desc";
    private static final String SELECT = "id,subject,bodyPreview,from,receivedDateTime,internetMessageHeaders";

    @Override
    public Provider provider() {
        return Provider.OUTLOOK;
    }

    @Override
    protected int batchLimit() {
        return BATCH_LIMIT;
    }

    @Override
    protected void deleteBatch(String accessToken, List<String> messageIds) {
        Http.postJson(ROOT + "/$batch", accessToken, batchBody(messageIds));
    }

    @Override
    protected List<String> listMessageIds(String accessToken, int maxMessages, FetchContext ctx) {
        List<String> ids = new ArrayList<>();
        String url = MESSAGES + "?$orderby=" + Http.enc(ORDER_BY) + "&$select=id&$top=" + PAGE_SIZE;
        while (url != null && ids.size() < maxMessages && !ctx.isCancelled()) {
            JsonNode page = Json.parse(Http.get(url, accessToken));
            page.path("value").forEach(m -> ids.add(m.path("id").asText()));
            url = page.path("@odata.nextLink").asText(null);
        }
        return ids.size() > maxMessages ? ids.subList(0, maxMessages) : ids;
    }

    @Override
    protected Optional<EmailMessage> fetchMessage(String accessToken, String id) {
        String url = MESSAGES + "/" + id + "?$select=" + Http.enc(SELECT);
        return Optional.of(toMessage(Json.parse(Http.get(url, accessToken))));
    }

    private EmailMessage toMessage(JsonNode node) {
        JsonNode from = node.path("from").path("emailAddress");
        String address = from.path("address").asText("").toLowerCase();
        String name = from.path("name").asText(address);
        var domain = Addresses.parseFrom(name + " <" + address + ">").domain();
        Map<String, String> headers = headerMap(node.path("internetMessageHeaders"));
        return new EmailMessage(
                node.path("id").asText(),
                name,
                address,
                domain,
                node.path("subject").asText("(no subject)"),
                node.path("bodyPreview").asText(""),
                parseDate(node.path("receivedDateTime").asText(null)),
                UnsubscribeHeaders.parse(headers.get("list-unsubscribe"), headers.get("list-unsubscribe-post"), domain),
                BulkMail.isBulk(headers));
    }

    private Map<String, String> headerMap(JsonNode headers) {
        Map<String, String> map = new HashMap<>();
        for (JsonNode h : headers) map.put(h.path("name").asText().toLowerCase(), h.path("value").asText());
        return map;
    }

    private Instant parseDate(String iso) {
        try {
            return iso == null ? Instant.EPOCH : Instant.parse(iso);
        } catch (RuntimeException e) {
            return Instant.EPOCH;
        }
    }

    private String batchBody(List<String> ids) {
        List<Map<String, Object>> requests = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            requests.add(Map.of("id", String.valueOf(i), "method", "DELETE",
                    "url", "/me/messages/" + ids.get(i)));
        }
        return Json.write(Map.of("requests", requests));
    }
}
