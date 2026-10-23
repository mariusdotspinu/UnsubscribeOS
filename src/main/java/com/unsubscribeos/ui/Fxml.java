package com.unsubscribeos.ui;

import javafx.fxml.FXMLLoader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.net.URL;

/**
 * Loads the views under {@code /fxml}. FXML owns the layout; the Java class named by
 * {@code fx:controller} owns the behaviour. Controllers that need runtime state (services, the
 * signed-in account, …) are built by the caller and passed in; any other controller — including
 * those of {@code fx:include}d files — is created through its no-arg constructor.
 */
public final class Fxml {

    private Fxml() {}

    public static <T> T load(String name, Object... controllers) {
        URL location = Fxml.class.getResource("/fxml/" + name);
        if (location == null) throw new IllegalArgumentException("Missing FXML: " + name);
        FXMLLoader loader = new FXMLLoader(location);
        loader.setControllerFactory(type -> controllerFor(type, controllers));
        try {
            return loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't load " + name, e);
        }
    }

    private static Object controllerFor(Class<?> type, Object[] controllers) {
        for (Object controller : controllers) {
            if (type.isInstance(controller)) return controller;
        }
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No controller supplied for " + type.getName(), e);
        }
    }
}
