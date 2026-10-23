package com.unsubscribeos.ui.component;

import com.unsubscribeos.core.model.Provider;
import com.unsubscribeos.ui.Fxml;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Scale;

/**
 * Provider brand logo as a scalable node. Prefers a bundled {@code /images/<provider>.png} when
 * present (drop in the official asset to upgrade), otherwise uses the brand-coloured vector badge
 * drawn in {@code logo-<provider>.fxml} so a recognisable logo always renders.
 */
public final class ProviderLogo {

    /** The vector badges are drawn on a 48×48 grid. */
    private static final double GRID = 48;

    private ProviderLogo() {}

    public static Node of(Provider provider, double size) {
        ImageView bundled = load(provider, size);
        return bundled != null ? bundled : vector(provider, size);
    }

    private static ImageView load(Provider provider, double size) {
        var url = ProviderLogo.class.getResource("/images/" + key(provider) + ".png");
        if (url == null) return null;
        ImageView view = new ImageView(new Image(url.toExternalForm()));
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }

    private static Node vector(Provider provider, double size) {
        Group badge = Fxml.load("logo-" + key(provider) + ".fxml");
        badge.getTransforms().add(new Scale(size / GRID, size / GRID));
        // Wrapped so layout sees the scaled size (a node's own transforms don't count toward it).
        return new Group(badge);
    }

    private static String key(Provider provider) {
        return provider.name().toLowerCase();
    }
}
