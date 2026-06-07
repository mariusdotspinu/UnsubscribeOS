package com.unsubscribeos.ui.component;

import com.unsubscribeos.core.model.Provider;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;

/**
 * Provider brand logo as a scalable node. Prefers a bundled {@code /images/<provider>.png} when
 * present (drop in the official asset to upgrade), otherwise draws a crisp brand-coloured vector
 * badge so a recognisable logo always renders. New providers only need an enum value here.
 */
public final class ProviderLogo {

    private ProviderLogo() {}

    public static Node of(Provider provider, double size) {
        ImageView bundled = load(provider, size);
        return bundled != null ? bundled : vector(provider, size);
    }

    private static ImageView load(Provider provider, double size) {
        var url = ProviderLogo.class.getResource("/images/" + provider.name().toLowerCase() + ".png");
        if (url == null) return null;
        ImageView view = new ImageView(new Image(url.toExternalForm()));
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }

    // ---- vector fallback ----------------------------------------------------

    private static Group vector(Provider provider, double size) {
        double k = size / 48.0;
        boolean gmail = provider == Provider.GMAIL;

        Rectangle badge = new Rectangle(size, size);
        badge.setArcWidth(14 * k);
        badge.setArcHeight(14 * k);
        badge.setFill(gmail ? Color.WHITE : Color.web("#0F6CBD"));
        if (gmail) {
            badge.setStroke(Color.web("#dadce0"));
            badge.setStrokeWidth(1);
        }

        // The shared envelope "M": four-colour for Gmail, white for Outlook.
        String[] colors = gmail
                ? new String[]{"#4285F4", "#EA4335", "#FBBC04", "#34A853"}
                : new String[]{"#FFFFFF", "#FFFFFF", "#FFFFFF", "#FFFFFF"};
        return new Group(badge,
                seg(11, 35, 11, 14, k, colors[0]),
                seg(11, 14, 24, 27, k, colors[1]),
                seg(24, 27, 37, 14, k, colors[2]),
                seg(37, 14, 37, 35, k, colors[3]));
    }

    private static Line seg(double x1, double y1, double x2, double y2, double k, String color) {
        Line line = new Line(x1 * k, y1 * k, x2 * k, y2 * k);
        line.setStroke(Color.web(color));
        line.setStrokeWidth(5 * k);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        return line;
    }
}
