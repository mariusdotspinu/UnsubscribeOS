package com.unsubscribeos.ui.component;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.util.List;

/**
 * The application's brand mark: a calm-blue tile with a white envelope (mail) and a minus badge
 * (unsubscribe / remove). Prefers the bundled {@code /icon.png}; if absent, falls back to the
 * equivalent vector drawing so an icon always renders.
 */
public final class AppIcon {

    private static final Image IMAGE = load();

    private AppIcon() {}

    private static Image load() {
        var url = AppIcon.class.getResource("/icon.png");
        return url == null ? null : new Image(url.toExternalForm());
    }

    /** The icon as a node at the given square size — usable anywhere in the UI. */
    public static Node node(double size) {
        if (IMAGE == null) return vector(size);
        ImageView view = new ImageView(IMAGE);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }

    /** Window/taskbar icons. */
    public static List<Image> icons() {
        return IMAGE != null ? List.of(IMAGE) : List.of(render(256), render(64), render(32));
    }

    // ---- vector fallback ----------------------------------------------------

    private static Image render(int size) {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return vector(size).snapshot(params, null);
    }

    private static Group vector(double size) {
        double k = size / 256.0;
        LinearGradient blue = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#5b9bff")), new Stop(1, Color.web("#3f6fe0")));

        Rectangle tile = rect(0, 0, 256, 256, 112, k);
        tile.setFill(blue);
        Rectangle envelope = rect(52, 74, 152, 108, 32, k);
        envelope.setFill(Color.WHITE);

        Polyline flap = new Polyline(60 * k, 86 * k, 128 * k, 138 * k, 196 * k, 86 * k);
        flap.setFill(null);
        flap.setStroke(Color.web("#cdddff"));
        flap.setStrokeWidth(12 * k);
        flap.setStrokeLineCap(StrokeLineCap.ROUND);
        flap.setStrokeLineJoin(StrokeLineJoin.ROUND);

        Circle badge = new Circle(192 * k, 180 * k, 40 * k, blue);
        badge.setStroke(Color.WHITE);
        badge.setStrokeWidth(10 * k);
        Rectangle minus = rect(174, 173.5, 36, 13, 13, k);
        minus.setFill(Color.WHITE);

        return new Group(tile, envelope, flap, badge, minus);
    }

    private static Rectangle rect(double x, double y, double w, double h, double arc, double k) {
        Rectangle rectangle = new Rectangle(x * k, y * k, w * k, h * k);
        rectangle.setArcWidth(arc * k);
        rectangle.setArcHeight(arc * k);
        return rectangle;
    }
}
