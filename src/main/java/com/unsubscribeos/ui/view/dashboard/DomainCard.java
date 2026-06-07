package com.unsubscribeos.ui.view.dashboard;

import com.unsubscribeos.core.mail.MailService;
import com.unsubscribeos.core.model.Account;
import com.unsubscribeos.core.model.EmailMessage;
import com.unsubscribeos.core.model.MailDomain;
import com.unsubscribeos.ui.AppContext;
import com.unsubscribeos.ui.Async;
import com.unsubscribeos.ui.Errors;
import com.unsubscribeos.ui.component.HeatScale;
import com.unsubscribeos.ui.component.Notifier;
import com.unsubscribeos.ui.component.Ui;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * One expandable sender-domain card. Built once, then {@link #update} refreshes it in place as
 * the live fetch streams in — keeping its count, heat colour and ordering current without losing
 * the user's expand / "show more" state. Exposes unsubscribe / delete actions for the whole
 * sender and per-email deletion; deletions are purged from the shared buffer so re-grouping stays
 * consistent.
 */
final class DomainCard {

    static final int INITIAL = 5;
    static final int PAGE = 10;

    private final AppContext context;
    private final Account account;
    private final MailService mail;
    private final Notifier notifier;
    private final Consumer<List<String>> purge;
    private final Runnable onRemoved;
    private final String domain;

    private List<EmailMessage> messages = new ArrayList<>();
    private boolean expanded;
    private int shown;

    private final VBox card = new VBox();
    private final VBox emailBox = new VBox();
    private final Region heat = new Region();
    private final Label countBadge = Ui.label("", "count-badge");
    private final Button moreButton = Ui.button("Show more", this::showMore, "ghost");

    DomainCard(AppContext context, Account account, MailService mail, String domain,
               Notifier notifier, Consumer<List<String>> purge, Runnable onRemoved) {
        this.context = context;
        this.account = account;
        this.mail = mail;
        this.domain = domain;
        this.notifier = notifier;
        this.purge = purge;
        this.onRemoved = onRemoved;
    }

    Node node() {
        return card;
    }

    void build() {
        heat.setMaxHeight(Double.MAX_VALUE);
        heat.getStyleClass().add("heat-bar");
        emailBox.setVisible(false);
        emailBox.setManaged(false);
        card.getStyleClass().add("domain-card");
        card.getChildren().setAll(header(), emailBox);
    }

    /** Replaces this card's contents with the latest snapshot for the domain. */
    void update(MailDomain snapshot, int maxCount) {
        messages = new ArrayList<>(snapshot.messages());
        countBadge.setText(String.valueOf(messages.size()));

        // Same heat grade drives the left bar (background) and the count badge (so volume stays
        // visible at a glance even when the list is sorted by name rather than by count).
        String heatClass = HeatScale.classFor(messages.size(), maxCount);
        heat.getStyleClass().removeIf(c -> c.startsWith("heat-"));
        heat.getStyleClass().add(heatClass);
        countBadge.getStyleClass().removeIf(c -> c.startsWith("vol-"));
        countBadge.getStyleClass().add(heatClass.replace("heat", "vol"));

        if (expanded) renderRows();
    }

    // ---- header -------------------------------------------------------------

    private HBox header() {
        HBox info = Ui.row(12, Pos.CENTER_LEFT, heat, Ui.label(domain, "section"), countBadge, Ui.grow());
        info.getStyleClass().add("domain-header");
        info.setOnMouseClicked(e -> toggle());
        HBox.setHgrow(info, Priority.ALWAYS);

        Button unsubscribe = Ui.icon("🔕", "Unsubscribe from this sender", this::unsubscribe);
        Button delete = Ui.icon("🗑", "Delete all emails from this sender", this::deleteDomain, "danger");
        return Ui.row(6, Pos.CENTER_LEFT, info, unsubscribe, delete);
    }

    private void toggle() {
        expanded = !expanded;
        if (expanded && shown == 0) shown = INITIAL;
        if (expanded) renderRows();
        emailBox.setVisible(expanded);
        emailBox.setManaged(expanded);
    }

    // ---- email rows ---------------------------------------------------------

    private void showMore() {
        shown = Math.min(shown + PAGE, messages.size());
        renderRows();
    }

    private void renderRows() {
        int visible = Math.min(shown, messages.size());
        List<Node> rows = new ArrayList<>();
        for (int i = 0; i < visible; i++) rows.add(emailRow(messages.get(i)));
        if (visible < messages.size()) rows.add(moreButton);
        emailBox.getChildren().setAll(rows);
    }

    private Node emailRow(EmailMessage message) {
        VBox text = new VBox(2, Ui.label(message.subject(), "email-subject"), Ui.label(message.snippet(), "email-snippet"));
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox row = Ui.row(10, Pos.CENTER_LEFT, text, Ui.grow());
        row.getChildren().add(Ui.icon("🗑", "Delete this email", () -> deleteEmail(message), "danger", "ghost"));
        row.getStyleClass().add("email-row");
        return row;
    }

    // ---- actions ------------------------------------------------------------

    private void unsubscribe() {
        notifier.info("Unsubscribing from " + domain + "…");
        var info = new MailDomain(domain, messages).bestUnsubscribe();
        Async.supply(() -> context.unsubscribeService().unsubscribe(info),
                result -> {
                    if (result.ok()) notifier.success(result.detail());
                    else notifier.error(result.detail());
                },
                error -> notifier.error("Unsubscribe failed: " + Errors.describe(error)));
    }

    private void deleteDomain() {
        delete(messages.stream().map(EmailMessage::id).toList(), this::remove);
    }

    private void deleteEmail(EmailMessage message) {
        delete(List.of(message.id()), () -> {
            messages.remove(message);
            if (messages.isEmpty()) {
                remove();
            } else {
                countBadge.setText(String.valueOf(messages.size()));
                if (expanded) renderRows();
            }
        });
    }

    private void delete(List<String> ids, Runnable onDeleted) {
        notifier.info("Deleting " + ids.size() + " email(s) from " + domain + "…");
        Async.run(() -> mail.delete(context.authService().accessToken(account), ids),
                () -> {
                    purge.accept(ids);
                    onDeleted.run();
                    notifier.success("Moved " + ids.size() + " email(s) to Trash.");
                },
                error -> notifier.error("Delete failed: " + Errors.describe(error)));
    }

    private void remove() {
        onRemoved.run();
    }
}
