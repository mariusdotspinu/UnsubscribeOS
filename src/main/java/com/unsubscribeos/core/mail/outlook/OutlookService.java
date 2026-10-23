package com.unsubscribeos.core.mail.outlook;

import com.fasterxml.jackson.databind.JsonNode;
import com.unsubscribeos.core.http.Http;
import com.unsubscribeos.core.http.HttpException;
import com.unsubscribeos.core.http.Json;
import com.unsubscribeos.core.mail.AbstractMailService;
import com.unsubscribeos.core.mail.Addresses;
import com.unsubscribeos.core.mail.FetchContext;
import com.unsubscribeos.core.mail.UnsubscribeHeaders;
import com.unsubscribeos.core.model.EmailMessage;
import com.unsubscribeos.core.model.Provider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Microsoft Graph provider. Mirrors Gmail: a cheap paged sweep collects the ids of received mail,
 * then each message is fetched concurrently; deletion uses Graph {@code $batch} to move messages to
 * Deleted Items (recoverable). The fetch/delete skeleton lives in {@link AbstractMailService}.
 */
public final class OutlookService extends AbstractMailService {

    private static final String ROOT = "https://graph.microsoft.com/v1.0";
    private static final String MESSAGES = ROOT + "/me/messages";
    private static final int PAGE_SIZE = 100;
    private static final int BATCH_LIMIT = 20;
    // Received mail only, mirroring Gmail's "-in:sent -in:chats". Graph's /me/messages spans the
    // *whole* mailbox — Sent Items, Drafts, Junk and Deleted Items included — which would group our
    // own outgoing mail under our own domain and offer to "delete" mail already in the bin. Listing
    // these two folders instead keeps the scan to mail other people sent us, archived mail included.
    private static final List<String> FOLDERS = List.of("inbox", "archive");
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
        List<Listed> listed = new ArrayList<>();
        for (String folder : FOLDERS) listed.addAll(listFolder(accessToken, folder, maxMessages, ctx));
        // Each folder arrives newest-first, but "the newest N overall" only holds once they're merged
        // by date — otherwise a deep Inbox would use up the whole budget before Archive is considered.
        return listed.stream()
                .sorted(Comparator.comparing(Listed::received).reversed())
                .limit(maxMessages)
                .map(Listed::id)
                .toList();
    }

    /** Ids + dates from one well-known folder, newest-first; empty if this mailbox has no such folder. */
    private List<Listed> listFolder(String accessToken, String folder, int maxMessages, FetchContext ctx) {
        List<Listed> listed = new ArrayList<>();
        String url = ROOT + "/me/mailFolders/" + folder + "/messages"
                + "?$orderby=" + Http.enc(ORDER_BY) + "&$select=id,receivedDateTime&$top=" + PAGE_SIZE;
        while (url != null && listed.size() < maxMessages && !ctx.isCancelled()) {
            String body;
            try {
                body = Http.get(url, accessToken);
            } catch (HttpException e) {
                // Archive isn't provisioned on every mailbox — "no such folder" just means no mail
                // there. Anything else is a real failure and must surface.
                if (e.status() == 404) break;
                throw e;
            }
            JsonNode page = Json.parse(body);
            page.path("value").forEach(m -> listed.add(new Listed(
                    m.path("id").asText(), parseDate(m.path("receivedDateTime").asText(null)))));
            url = page.path("@odata.nextLink").asText(null);
        }
        return listed;
    }

    /** One row of a folder listing — just enough to merge folders newest-first before the cap. */
    private record Listed(String id, Instant received) {}

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
                UnsubscribeHeaders.parse(headers.get("list-unsubscribe"), headers.get("list-unsubscribe-post"), domain));
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
