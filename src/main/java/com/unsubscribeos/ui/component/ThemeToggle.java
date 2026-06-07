package com.unsubscribeos.ui.component;

import com.unsubscribeos.ui.ThemeManager;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

/** A theme switch button shared by every screen; its glyph follows the active theme. */
public final class ThemeToggle {

    private ThemeToggle() {}

    public static Button button(ThemeManager themeManager) {
        Button button = Ui.icon(
                themeManager.current().toggleGlyph(),
                themeManager.current().toggleTooltip(),
                themeManager::toggle, "ghost");
        themeManager.themeProperty().addListener((obs, was, now) -> {
            button.setText(now.toggleGlyph());
            button.setTooltip(new Tooltip(now.toggleTooltip()));
        });
        return button;
    }
}
