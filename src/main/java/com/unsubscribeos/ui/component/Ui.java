package com.unsubscribeos.ui.component;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

/** Concise factory helpers for styled JavaFX nodes, keeping view code declarative. */
public final class Ui {

    private Ui() {}

    public static Label label(String text, String... styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClasses);
        return label;
    }

    public static Button button(String text, Runnable onClick, String... styleClasses) {
        Button button = new Button(text);
        button.getStyleClass().addAll(styleClasses);
        button.setOnAction(e -> onClick.run());
        return button;
    }

    public static Button icon(String glyph, String tooltip, Runnable onClick, String... extraClasses) {
        Button button = button(glyph, onClick, "icon");
        button.getStyleClass().addAll(extraClasses);
        button.setTooltip(new Tooltip(tooltip));
        return button;
    }

    public static Region grow() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    public static HBox row(double spacing, Pos alignment, Node... children) {
        HBox box = new HBox(spacing, children);
        box.setAlignment(alignment);
        return box;
    }

    public static <T extends Node> T with(T node, Consumer<T> config) {
        config.accept(node);
        return node;
    }
}
