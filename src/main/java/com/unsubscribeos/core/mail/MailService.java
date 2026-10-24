package com.unsubscribeos.core.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.unsubscribeos.core.mail.gmail.GmailService;
import com.unsubscribeos.core.mail.outlook.OutlookService;
import com.unsubscribeos.core.model.EmailMessage;
import com.unsubscribeos.core.model.FetchProgress;
import com.unsubscribeos.core.model.Provider;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A provider's mail REST API. Both supported providers follow the same shape — list the ids of
 * received mail, fetch each message concurrently, delete in provider-sized batches — so that
 * skeleton lives here once (Template Method); subclasses supply only endpoints and parsing.
 */
public abstract class MailService {

    /** Bounds in-flight message fetches to stay within provider rate limits. */
    private static final int MAX_IN_FLIGHT = 12;

    private final int batchLimit;

    protected MailService(int batchLimit) {
        this.batchLimit = batchLimit;
    }

    public static MailService of(Provider provider) {
        return switch (provider) {
            case GMAIL -> new GmailService();
            case OUTLOOK -> new OutlookService();
        };
    }

    /**
     * Streams recent messages to the context's callbacks, scanning at most {@code maxMessages}
     * (newest first) on virtual threads, with a running x/y progress count. Blocking — run off the
     * UI thread.
     */
    public final void fetch(String accessToken, int maxMessages, FetchContext ctx) {
        List<String> ids = listMessageIds(accessToken, maxMessages, ctx);
        int total = ids.size();
        AtomicInteger done = new AtomicInteger();
        Semaphore gate = new Semaphore(MAX_IN_FLIGHT);
        ctx.onProgress().accept(new FetchProgress(0, total));
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) { // close() awaits every task
            ids.forEach(id -> pool.submit(() -> {
                if (ctx.isCancelled()) return;
                // Already-seen ids skip the body fetch, which keeps the live polls cheap.
                if (ctx.shouldFetch().test(id)) fetchOne(accessToken, id, ctx, gate);
                ctx.onProgress().accept(new FetchProgress(done.incrementAndGet(), total));
            }));
        }
    }

    /** Moves the given messages to the provider's trash/deleted folder (recoverable). */
    public final void delete(String accessToken, List<String> ids) {
        for (int from = 0; from < ids.size(); from += batchLimit) {
            deleteBatch(accessToken, ids.subList(from, Math.min(ids.size(), from + batchLimit)));
        }
    }

    /** Lists the ids of the messages in scope (mail we received), paging up to {@code maxMessages}. */
    protected abstract List<String> listMessageIds(String accessToken, int maxMessages, FetchContext ctx);

    protected abstract EmailMessage fetchMessage(String accessToken, String id);

    /** Moves one batch of at most {@code batchLimit} messages to the trash. */
    protected abstract void deleteBatch(String accessToken, List<String> ids);

    /** A message from its raw parts; {@code headers} is keyed by lower-cased name (see {@link #headers}). */
    protected static EmailMessage message(String id, String from, String subject, String snippet,
                                          Instant date, Map<String, String> headers) {
        String domain = domainOf(from);
        return new EmailMessage(id, domain, subject, snippet, date, UnsubscribeHeaders.parse(
                headers.get("list-unsubscribe"), headers.get("list-unsubscribe-post"), domain));
    }

    /** A provider's {@code [{name, value}]} header list as a map keyed by lower-cased name. */
    protected static Map<String, String> headers(JsonNode list) {
        Map<String, String> map = new HashMap<>();
        list.forEach(h -> map.put(h.path("name").asText().toLowerCase(), h.path("value").asText()));
        return map;
    }

    /** The lower-cased domain of {@code Name <user@domain>} or {@code user@domain}; "unknown" if absent. */
    static String domainOf(String from) {
        if (from == null) return "unknown";
        int open = from.indexOf('<');
        int close = from.indexOf('>');
        String address = (open >= 0 && close > open ? from.substring(open + 1, close) : from).trim().toLowerCase();
        int at = address.indexOf('@');
        String domain = at >= 0 && at < address.length() - 1 ? address.substring(at + 1) : "";
        return domain.isBlank() ? "unknown" : domain;
    }

    private void fetchOne(String accessToken, String id, FetchContext ctx, Semaphore gate) {
        try {
            gate.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            ctx.onMessage().accept(fetchMessage(accessToken, id));
        } catch (RuntimeException ignored) {
            // Skip a single unreadable message rather than failing the whole fetch.
        } finally {
            gate.release();
        }
    }
}
