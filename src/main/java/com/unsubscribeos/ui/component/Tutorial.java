package com.unsubscribeos.ui.component;

import com.unsubscribeos.core.platform.Browser;
import com.unsubscribeos.ui.Fxml;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.regex.Pattern;

public final class Tutorial {

    private static final Pattern URL = Pattern.compile("https?://\\S+");

    @FXML private VBox box;
    @FXML private TextArea body;

    public void setText(String text) {
        body.setText(text);
        body.setPrefRowCount((int) text.lines().count() + 1);
        box.getChildren().setAll(body);
        box.getChildren().addAll(links(text));
    }

    static List<String> urls(String text) {
        return URL.matcher(text).results().map(match -> strip(match.group())).distinct().toList();
    }

    private static List<Hyperlink> links(String text) {
        return urls(text).stream().map(Tutorial::link).toList();
    }

    private static Hyperlink link(String url) {
        Hyperlink link = Fxml.load("tutorial-link.fxml");
        link.setText(url);
        link.setOnAction(e -> openOrCopy(link, url));
        return link;
    }

    private static void openOrCopy(Hyperlink link, String url) {
        try {
            Browser.open(url);
        } catch (RuntimeException unavailable) {
            ClipboardContent content = new ClipboardContent();
            content.putString(url);
            Clipboard.getSystemClipboard().setContent(content);
            link.setTooltip(new Tooltip("Couldn't open a browser — link copied to clipboard"));
        }
    }

    private static String strip(String url) {
        int end = url.length();
        while (end > 0 && ".,);:".indexOf(url.charAt(end - 1)) >= 0) end--;
        return url.substring(0, end);
    }
}
