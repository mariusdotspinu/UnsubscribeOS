package com.unsubscribeos.core.mail;

import com.unsubscribeos.core.model.EmailMessage;

import java.util.List;
import java.util.Optional;

/**
 * Template Method base for REST mail providers. Both supported providers follow the same shape —
 * list the ids of bulk mail, fetch each message concurrently, and delete in provider-sized
 * batches — so that skeleton lives here once. Subclasses fill in only the provider-specific
 * endpoints and parsing.
 */
public abstract class AbstractMailService implements MailService {

    @Override
    public final void fetch(String accessToken, int maxMessages, FetchContext context) {
        List<String> ids = listMessageIds(accessToken, maxMessages, context);
        ConcurrentFetcher.fetchAll(ids, id -> fetchMessage(accessToken, id), context);
    }

    @Override
    public final void delete(String accessToken, List<String> messageIds) {
        for (List<String> batch : Chunks.of(messageIds, batchLimit())) {
            deleteBatch(accessToken, batch);
        }
    }

    /** Lists the ids of the messages in scope (bulk/marketing mail), paging up to {@code maxMessages}. */
    protected abstract List<String> listMessageIds(String accessToken, int maxMessages, FetchContext context);

    /** Fetches and parses a single message, or empty if it can't be read. */
    protected abstract Optional<EmailMessage> fetchMessage(String accessToken, String id);

    /** Moves one batch of messages to the provider's trash/deleted folder (recoverable). */
    protected abstract void deleteBatch(String accessToken, List<String> messageIds);

    /** Maximum ids the provider accepts per delete request. */
    protected abstract int batchLimit();
}
