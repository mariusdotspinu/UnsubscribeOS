package com.unsubscribeos.core.mail;

import java.util.List;
import java.util.stream.IntStream;

/** Splits a list into fixed-size sublists — used for provider batch-size limits. */
public final class Chunks {

    private Chunks() {}

    public static <T> List<List<T>> of(List<T> items, int size) {
        if (items.isEmpty()) return List.of();
        return IntStream.range(0, (items.size() + size - 1) / size)
                .mapToObj(i -> items.subList(i * size, Math.min(items.size(), (i + 1) * size)))
                .toList();
    }
}
