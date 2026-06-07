package com.unsubscribeos.core.mail.outlook;

import com.fasterxml.jackson.databind.JsonNode;
import com.unsubscribeos.core.http.Http;
import com.unsubscribeos.core.http.Json;
import com.unsubscribeos.core.mail.AbstractMailService;
import com.unsubscribeos.core.mail.Addresses;
import com.unsubscribeos.core.mail.FetchContext;
import com.unsubscribeos.core.mail.UnsubscribeHeaders;
import com.unsubscribeos.core.model.EmailMessage;
import com.unsubscribeos.core.model.Provider;
import com.unsubscribeos.core.model.UnsubscribeInfo;

import java.time.Instant;
import java.util.ArrayList;
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
    private static final int MAX_MESSAGES = 1500;
    private static final int PAGE_SIZE = 100;
    private static final int BATCH_LIMIT = 20;
    private static final String SEARCH = "\"unsubscribe\"";
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
    protected List<String> listMessageIds(String accessToken, FetchContext ctx) {
        List<String> ids = new ArrayList<>();
        String url = MESSAGES + "?$search=" + Http.enc(SEARCH) + "&$select=id&$top=" + PAGE_SIZE;
        while (url != null && ids.size() < MAX_MESSAGES && !ctx.isCancelled()) {
            JsonNode page = Json.parse(Http.get(url, accessToken));
            page.path("value").forEach(m -> ids.add(m.path("id").asText()));
            url = page.path("@odata.nextLink").asText(null);
        }
        return ids.size() > MAX_MESSAGES ? ids.subList(0, MAX_MESSAGES) : ids;
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
        return new EmailMessage(
                node.path("id").asText(),
                name,
                address,
                Addresses.parseFrom(name + " <" + address + ">").domain(),
                node.path("subject").asText("(no subject)"),
                node.path("bodyPreview").asText(""),
                parseDate(node.path("receivedDateTime").asText(null)),
                parseHeaders(node.path("internetMessageHeaders")));
    }

    private Optional<UnsubscribeInfo> parseHeaders(JsonNode headers) {
        String listUnsub = null, listUnsubPost = null;
        for (JsonNode h : headers) {
            String name = h.path("name").asText().toLowerCase();
            if (name.equals("list-unsubscribe")) listUnsub = h.path("value").asText();
            else if (name.equals("list-unsubscribe-post")) listUnsubPost = h.path("value").asText();
        }
        return UnsubscribeHeaders.parse(listUnsub, listUnsubPost);
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
