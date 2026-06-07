package com.unsubscribeos.core.unsubscribe;

/** Outcome of an unsubscribe attempt, so the UI can give precise feedback. */
public record UnsubscribeResult(Status status, String detail) {

    public enum Status {
        ONE_CLICK,      // RFC 8058 HTTPS POST sent successfully
        OPENED_BROWSER, // an unsubscribe page was opened for the user to confirm
        OPENED_MAIL,    // a pre-filled unsubscribe email was opened
        UNAVAILABLE,    // no unsubscribe information found
        FAILED          // an attempt was made but errored
    }

    public boolean ok() {
        return status != Status.FAILED && status != Status.UNAVAILABLE;
    }

    static UnsubscribeResult of(Status status, String detail) {
        return new UnsubscribeResult(status, detail);
    }
}
