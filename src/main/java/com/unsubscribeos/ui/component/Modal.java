package com.unsubscribeos.ui.component;

import com.unsubscribeos.ui.Fxml;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Controller for {@code modal.fxml}: a lightweight in-scene modal — a dimmed backdrop over the
 * host with a centered, themed card. Dismisses on the close button, a click outside the card, or
 * Escape. Rendered inside the scene (rather than a separate Stage) so it inherits the app theme
 * and avoids window-manager quirks.
 */
public final class Modal {

    private final StackPane host;

    @FXML private StackPane overlay;
    @FXML private Label title;
    @FXML private Tutorial bodyController;

    private Modal(StackPane host) {
        this.host = host;
    }

    public static void show(StackPane host, String title, String body) {
        Modal modal = new Modal(host);
        StackPane overlay = Fxml.load("modal.fxml", modal);
        modal.title.setText(title);
        modal.bodyController.setText(body);

        host.getChildren().add(overlay);
        overlay.requestFocus();
        fade(overlay, 0, 1, null);
    }

    @FXML
    private void dismiss() {
        fade(overlay, overlay.getOpacity(), 0, () -> host.getChildren().remove(overlay));
    }

    @FXML
    private void backdropClicked(MouseEvent e) {
        if (e.getTarget() == overlay) dismiss();
    }

    @FXML
    private void keyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.ESCAPE) dismiss();
    }

    private static void fade(Node node, double from, double to, Runnable after) {
        FadeTransition transition = new FadeTransition(Duration.millis(150), node);
        transition.setFromValue(from);
        transition.setToValue(to);
        if (after != null) transition.setOnFinished(e -> after.run());
        transition.play();
    }
}
