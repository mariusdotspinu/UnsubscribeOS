package com.unsubscribeos.core.unsubscribe;

/** Outcome of an unsubscribe attempt, so the UI can give precise feedback. */
public record UnsubscribeResult(boolean ok, String detail) {
}
