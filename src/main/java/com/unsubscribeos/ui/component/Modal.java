package com.unsubscribeos.ui.component;

import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * A lightweight in-scene modal: a dimmed backdrop over the host with a centered, themed card.
 * Dismisses on the close button, a click outside the card, or Escape. Rendered inside the scene
 * (rather than a separate Stage) so it inherits the app theme and avoids window-manager quirks.
 */
public final class Modal {

    private Modal() {}

    public static void show(StackPane host, String title, String body) {
        Button close = Ui.button("Got it", () -> {}, "primary");
        VBox card = new VBox(16,
                Ui.label(title, "modal-title"),
                Tutorial.of(body),
                Ui.row(0, Pos.CENTER_RIGHT, close));
        card.getStyleClass().add("modal-card");
        card.setMaxWidth(480);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane overlay = new StackPane(card);
        overlay.getStyleClass().add("modal-backdrop");
        StackPane.setAlignment(card, Pos.CENTER);

        Runnable dismiss = () -> dismiss(host, overlay);
        close.setOnAction(e -> dismiss.run());
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) dismiss.run(); });
        overlay.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) dismiss.run(); });

        host.getChildren().add(overlay);
        overlay.setFocusTraversable(true);
        overlay.requestFocus();
        fade(overlay, 0, 1, null);
    }

    private static void dismiss(StackPane host, StackPane overlay) {
        fade(overlay, overlay.getOpacity(), 0, () -> host.getChildren().remove(overlay));
    }

    private static void fade(Node node, double from, double to, Runnable after) {
        FadeTransition transition = new FadeTransition(Duration.millis(150), node);
        transition.setFromValue(from);
        transition.setToValue(to);
        if (after != null) transition.setOnFinished(e -> after.run());
        transition.play();
    }
}
