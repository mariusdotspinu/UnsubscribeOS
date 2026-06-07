package com.unsubscribeos.core.http;

/** Unchecked transport/HTTP error, so the functional fetch pipeline stays free of checked plumbing. */
public class HttpException extends RuntimeException {
    public HttpException(String message) { super(message); }
    public HttpException(String message, Throwable cause) { super(message, cause); }
}
