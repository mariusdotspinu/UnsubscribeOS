package com.unsubscribeos.ui.component;

import com.unsubscribeos.ui.Fxml;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;

import java.util.List;

/**
 * Window/taskbar icons for the application's brand mark. Prefers the bundled {@code /icon.png}
 * (which the views also show directly); if absent, renders the vector drawing in
 * {@code app-icon.fxml} so an icon always appears.
 */
public final class AppIcon {

    /** The vector fallback is drawn on a 256×256 grid. */
    private static final double GRID = 256;

    private static final Image IMAGE = load();

    private AppIcon() {}

    private static Image load() {
        var url = AppIcon.class.getResource("/icon.png");
        return url == null ? null : new Image(url.toExternalForm());
    }

    public static List<Image> icons() {
        return IMAGE != null ? List.of(IMAGE) : List.of(render(256), render(64), render(32));
    }

    private static Image render(int size) {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setTransform(Transform.scale(size / GRID, size / GRID));
        Node vector = Fxml.load("app-icon.fxml");
        return vector.snapshot(params, null);
    }
}
