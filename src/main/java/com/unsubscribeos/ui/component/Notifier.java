package com.unsubscribeos.ui.component;

/** Surface for transient user feedback, decoupled from how it is presented. */
public interface Notifier {

    void info(String message);

    void success(String message);

    void error(String message);
}
