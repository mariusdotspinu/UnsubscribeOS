package com.unsubscribeos.ui;

import com.unsubscribeos.config.Settings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;

/**
 * Owns the active {@link Theme}, applies its stylesheets to the scene, and persists the
 * choice. Exposes an observable property so theme toggle buttons across views stay in sync.
 */
public final class ThemeManager {

    private static final String APP_CSS = "/css/app.css";
    private static final String KEY = "theme";

    private final Settings settings;
    private final ObjectProperty<Theme> theme;
    private Scene scene;

    public ThemeManager(Settings settings) {
        this.settings = settings;
        this.theme = new SimpleObjectProperty<>(initialTheme(settings));
    }

    public void attach(Scene scene) {
        this.scene = scene;
        apply(theme.get());
    }

    public ObjectProperty<Theme> themeProperty() {
        return theme;
    }

    public Theme current() {
        return theme.get();
    }

    public void toggle() {
        Theme next = theme.get().other();
        theme.set(next);
        apply(next);
        settings.put(KEY, next.name());
    }

    private void apply(Theme t) {
        if (scene == null) return;
        scene.getStylesheets().setAll(css(APP_CSS), css(t.stylesheet()));
    }

    private String css(String resource) {
        return ThemeManager.class.getResource(resource).toExternalForm();
    }

    private static Theme initialTheme(Settings settings) {
        return settings.get(KEY).map(ThemeManager::parse).orElse(Theme.DARK);
    }

    private static Theme parse(String name) {
        try {
            return Theme.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Theme.DARK;
        }
    }
}
