package com.unsubscribeos.ui;

/** Turns a throwable into a short, human-readable message for status lines and toasts. */
public final class Errors {

    private Errors() {}

    public static String describe(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
