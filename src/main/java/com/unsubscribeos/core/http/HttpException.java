package com.unsubscribeos.core.http;

/**
 * Unchecked transport/HTTP error, so the functional fetch pipeline stays free of checked plumbing.
 * {@link #status()} carries the HTTP status when the server answered, letting callers react to a
 * specific code (e.g. treating a 404 as "this optional resource simply isn't there") without
 * parsing messages; it is {@code 0} when the request never got a response at all.
 */
public class HttpException extends RuntimeException {

    private final int status;

    public HttpException(String message, int status) {
        super(message);
        this.status = status;
    }

    public HttpException(String message, Throwable cause) {
        super(message, cause);
        this.status = 0;
    }

    /** The HTTP status the server returned, or {@code 0} for a transport failure. */
    public int status() {
        return status;
    }
}
