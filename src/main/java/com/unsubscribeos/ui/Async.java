package com.unsubscribeos.ui;

import javafx.application.Platform;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Async {

    private Async() {}

    public static <T> void supply(Supplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Thread.ofVirtual().start(() -> {
            try {
                T result = work.get();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Throwable t) {
                Platform.runLater(() -> onError.accept(t));
            }
        });
    }

    public static void run(Runnable work, Runnable onSuccess, Consumer<Throwable> onError) {
        supply(() -> { work.run(); return null; }, ignored -> onSuccess.run(), onError);
    }
}
