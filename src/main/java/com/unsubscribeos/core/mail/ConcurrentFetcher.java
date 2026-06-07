package com.unsubscribeos.core.mail;

import com.unsubscribeos.core.model.EmailMessage;
import com.unsubscribeos.core.model.FetchProgress;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Maps a list of message ids to {@link EmailMessage}s concurrently on virtual threads,
 * bounding in-flight requests with a semaphore to stay within provider rate limits, while
 * streaming each result and a running x/y progress count back through the {@link FetchContext}.
 */
public final class ConcurrentFetcher {

    private static final int MAX_IN_FLIGHT = 12;

    private ConcurrentFetcher() {}

    public static void fetchAll(List<String> ids, Function<String, Optional<EmailMessage>> mapper, FetchContext ctx) {
        int total = ids.size();
        ctx.onProgress().accept(new FetchProgress(0, total));
        AtomicInteger done = new AtomicInteger();
        Semaphore gate = new Semaphore(MAX_IN_FLIGHT);

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = ids.stream()
                    .map(id -> pool.submit(() -> process(id, mapper, ctx, gate, done, total)))
                    .toList();
            awaitAll(tasks);
        }
    }

    private static void process(String id, Function<String, Optional<EmailMessage>> mapper, FetchContext ctx,
                                Semaphore gate, AtomicInteger done, int total) {
        if (ctx.isCancelled()) return;
        if (!ctx.shouldFetch().test(id)) {
            ctx.onProgress().accept(new FetchProgress(done.incrementAndGet(), total));
            return; // already have this message — skip the body fetch (keeps polls cheap)
        }
        try {
            gate.acquire();
            mapper.apply(id).ifPresent(ctx.onMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException ignored) {
            // Skip a single unreadable message rather than failing the whole fetch.
        } finally {
            gate.release();
            ctx.onProgress().accept(new FetchProgress(done.incrementAndGet(), total));
        }
    }

    private static void awaitAll(List<? extends Future<?>> tasks) {
        for (Future<?> task : tasks) {
            try {
                task.get();
            } catch (Exception ignored) {
                // individual failures already handled in process()
            }
        }
    }
}
