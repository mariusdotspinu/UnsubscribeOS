package com.unsubscribeos.ui.component;

import com.unsubscribeos.core.model.Provider;
import com.unsubscribeos.ui.Fxml;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.transform.Scale;

public final class ProviderLogo {

    private static final double GRID = 48;

    private ProviderLogo() {}

    public static Node of(Provider provider, double size) {
        Group badge = Fxml.load("logo-" + provider.name().toLowerCase() + ".fxml");
        badge.getTransforms().add(new Scale(size / GRID, size / GRID));
        // Wrapped so layout sees the scaled size.
        return new Group(badge);
    }
}
