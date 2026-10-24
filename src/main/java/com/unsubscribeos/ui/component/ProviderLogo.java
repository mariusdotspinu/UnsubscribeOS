package com.unsubscribeos.ui.component;

import com.unsubscribeos.core.model.Provider;
import com.unsubscribeos.ui.Fxml;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.transform.Scale;

/** Provider brand logo: the vector badge drawn in {@code logo-<provider>.fxml}, scaled to size. */
public final class ProviderLogo {

    /** The vector badges are drawn on a 48×48 grid. */
    private static final double GRID = 48;

    private ProviderLogo() {}

    public static Node of(Provider provider, double size) {
        Group badge = Fxml.load("logo-" + provider.name().toLowerCase() + ".fxml");
        badge.getTransforms().add(new Scale(size / GRID, size / GRID));
        // Wrapped so layout sees the scaled size (a node's own transforms don't count toward it).
        return new Group(badge);
    }
}
