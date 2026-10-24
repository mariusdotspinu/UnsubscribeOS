package com.unsubscribeos.ui.component;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Controller for {@code toast.fxml}: a prominent, self-dismissing notification. Each message fades
 * in, shows a draining progress bar for a few seconds, then fades out automatically; a new message
 * restarts the timer, and a click dismisses it early.
 */
public final class Toast {

    /** Severity, mapped to colour and glyph through the CSS classes of the same name. */
    private enum Level {
        INFO("info", "ℹ"), SUCCESS("success", "✓"), ERROR("error", "⚠");

        final String styleClass;
        final String glyph;

        Level(String styleClass, String glyph) {
            this.styleClass = styleClass;
            this.glyph = glyph;
        }
    }

    private static final Duration VISIBLE = Duration.seconds(4);

    @FXML private Label icon;
    @FXML private Label message;
    @FXML private ProgressBar timer;
    @FXML private VBox container;

    private Timeline countdown;
    private Animation fade;

    public void info(String text)    { show(text, Level.INFO); }
    public void success(String text) { show(text, Level.SUCCESS); }
    public void error(String text)   { show(text, Level.ERROR); }

    private void show(String text, Level level) {
        if (countdown != null) countdown.stop();
        if (fade != null) fade.stop();
        message.setText(text);
        icon.setText(level.glyph);
        container.getStyleClass().removeAll("info", "success", "error");
        container.getStyleClass().add(level.styleClass);

        container.setVisible(true);
        container.setManaged(true);
        container.setOpacity(0);
        fade = Modal.fade(container, 1);
        fade.play();

        timer.setProgress(1);
        countdown = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(timer.progressProperty(), 1.0)),
                new KeyFrame(VISIBLE, new KeyValue(timer.progressProperty(), 0.0)));
        countdown.setOnFinished(e -> hide());
        countdown.play();
    }

    @FXML
    private void hide() {
        if (countdown != null) countdown.stop();
        fade = Modal.fade(container, 0);
        fade.setOnFinished(e -> {
            container.setVisible(false);
            container.setManaged(false);
        });
        fade.play();
    }
}
