package com.unsubscribeos.ui;

public final class Errors {

    private Errors() {}

    public static String describe(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
