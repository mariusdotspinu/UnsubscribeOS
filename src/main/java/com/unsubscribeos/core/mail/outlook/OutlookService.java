package com.unsubscribeos.core.mail.outlook;

import com.fasterxml.jackson.databind.JsonNode;
import com.unsubscribeos.core.http.Http;
import com.unsubscribeos.core.http.HttpException;
import com.unsubscribeos.core.http.Json;
import com.unsubscribeos.core.mail.FetchContext;
import com.unsubscribeos.core.mail.MailService;
import com.unsubscribeos.core.model.EmailMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public final class OutlookService extends MailService {

    private static final String ROOT = "https://graph.microsoft.com/v1.0";
    private static final String MESSAGES = ROOT + "/me/messages";
    private static final int PAGE_SIZE = 100;
    // Inbox + Archive only: /me/messages also spans Sent, Drafts, Junk and Deleted Items.
    private static final List<String> FOLDERS = List.of("inbox", "archive");
    private static final String ORDER_BY = "receivedDateTime desc";
    private static final String SELECT = "id,subject,bodyPreview,from,receivedDateTime,internetMessageHeaders";

    public OutlookService() {
        super(20);
    }

    @Override
    protected void deleteBatch(String accessToken, List<String> ids) {
        List<Map<String, String>> requests = IntStream.range(0, ids.size())
                .mapToObj(i -> Map.of("id", String.valueOf(i), "method", "DELETE", "url", "/me/messages/" + ids.get(i)))
                .toList();
        Http.postJson(ROOT + "/$batch", accessToken, Json.write(Map.of("requests", requests)));
    }

    @Override
    protected List<String> listMessageIds(String accessToken, int maxMessages, FetchContext ctx) {
        List<Listed> listed = new ArrayList<>();
        for (String folder : FOLDERS) listed.addAll(listFolder(accessToken, folder, maxMessages, ctx));
        // Merge by date so a deep Inbox can't use the whole budget before Archive.
        return listed.stream()
                .sorted(Comparator.comparing(Listed::received).reversed())
                .limit(maxMessages)
                .map(Listed::id)
                .toList();
    }

    private List<Listed> listFolder(String accessToken, String folder, int maxMessages, FetchContext ctx) {
        List<Listed> listed = new ArrayList<>();
        String url = ROOT + "/me/mailFolders/" + folder + "/messages"
                + "?$orderby=" + Http.enc(ORDER_BY) + "&$select=id,receivedDateTime&$top=" + PAGE_SIZE;
        while (url != null && listed.size() < maxMessages && !ctx.isCancelled()) {
            String body;
            try {
                body = Http.get(url, accessToken);
            } catch (HttpException e) {
                // Archive isn't provisioned on every mailbox; a 404 just means no mail there.
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

    private record Listed(String id, Instant received) {}

    @Override
    protected EmailMessage fetchMessage(String accessToken, String id) {
        JsonNode node = Json.parse(Http.get(MESSAGES + "/" + id + "?$select=" + Http.enc(SELECT), accessToken));
        return message(node.path("id").asText(), node.path("from").path("emailAddress").path("address").asText(""),
                node.path("subject").asText("(no subject)"), node.path("bodyPreview").asText(""),
                parseDate(node.path("receivedDateTime").asText(null)), headers(node.path("internetMessageHeaders")));
    }

    private Instant parseDate(String iso) {
        try {
            return iso == null ? Instant.EPOCH : Instant.parse(iso);
        } catch (RuntimeException e) {
            return Instant.EPOCH;
        }
    }
}
