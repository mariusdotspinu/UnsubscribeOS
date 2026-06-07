package com.unsubscribeos.core.model;

/** Immutable snapshot of fetch progress, surfaced to the live progress bar as x/y. */
public record FetchProgress(int fetched, int total) {

    public double ratio() {
        return total <= 0 ? 0 : Math.min(1.0, (double) fetched / total);
    }

    public boolean done() {
        return total > 0 && fetched >= total;
    }
}
