package com.unsubscribeos.core.http;

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

    /** HTTP status, or 0 if no response arrived. */
    public int status() {
        return status;
    }
}
