package com.unsubscribeos.ui.component;

import com.unsubscribeos.core.platform.Browser;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders instructional text the user can read, select and copy (a read-only {@link TextArea}),
 * with any embedded URLs surfaced beneath as clickable links that open in the system browser.
 */
public final class Tutorial {

    private static final Pattern URL = Pattern.compile("https?://\\S+");

    private Tutorial() {}

    public static Node of(String text) {
        TextArea body = new TextArea(text);
        body.setEditable(false);
        body.setWrapText(true);
        body.setFocusTraversable(false);
        body.setPrefRowCount((int) text.lines().count() + 1);
        body.getStyleClass().add("tutorial");

        VBox box = new VBox(10, body);
        box.getChildren().addAll(links(text));
        return box;
    }

    /** Unique URLs found in the text, in first-seen order. Package-private for testing. */
    static List<String> urls(String text) {
        Set<String> urls = new LinkedHashSet<>();
        Matcher matcher = URL.matcher(text);
        while (matcher.find()) urls.add(strip(matcher.group()));
        return new ArrayList<>(urls);
    }

    private static List<Hyperlink> links(String text) {
        return urls(text).stream().map(Tutorial::link).toList();
    }

    private static Hyperlink link(String url) {
        Hyperlink link = new Hyperlink(url);
        link.getStyleClass().add("tutorial-link");
        link.setOnAction(e -> openOrCopy(link, url));
        return link;
    }

    /** Opens the link, or — if no browser can be launched — copies it to the clipboard. */
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

    /** Drops trailing sentence punctuation that the greedy match may have swept up. */
    private static String strip(String url) {
        int end = url.length();
        while (end > 0 && ".,);:".indexOf(url.charAt(end - 1)) >= 0) end--;
        return url.substring(0, end);
    }
}
