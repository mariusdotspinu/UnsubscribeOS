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
        overlay.setOpacity(0);
        fade(overlay, 1).play();
    }

    @FXML
    private void dismiss() {
        FadeTransition out = fade(overlay, 0);
        out.setOnFinished(e -> host.getChildren().remove(overlay));
        out.play();
    }

    @FXML
    private void backdropClicked(MouseEvent e) {
        if (e.getTarget() == overlay) dismiss();
    }

    @FXML
    private void keyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.ESCAPE) dismiss();
    }

    static FadeTransition fade(Node node, double to) {
        FadeTransition transition = new FadeTransition(Duration.millis(160), node);
        transition.setFromValue(node.getOpacity());
        transition.setToValue(to);
        return transition;
    }
}
