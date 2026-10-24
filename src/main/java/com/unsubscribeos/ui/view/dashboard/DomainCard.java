package com.unsubscribeos.ui.view.dashboard;

import com.unsubscribeos.core.mail.MailService;
import com.unsubscribeos.core.model.Account;
import com.unsubscribeos.core.model.EmailMessage;
import com.unsubscribeos.core.model.MailDomain;
import com.unsubscribeos.ui.AppContext;
import com.unsubscribeos.ui.Async;
import com.unsubscribeos.ui.Errors;
import com.unsubscribeos.ui.Fxml;
import com.unsubscribeos.ui.component.HeatScale;
import com.unsubscribeos.ui.component.Toast;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controller for {@code domain-card.fxml}: one expandable sender-domain card. Built once, then {@link #update} refreshes it in place as
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
    private final Toast notifier;
    private final Consumer<List<String>> purge;
    private final Runnable onRemoved;
    private final String domain;

    private List<EmailMessage> messages = new ArrayList<>();
    private boolean expanded;
    private int shown;

    @FXML private VBox card;
    @FXML private VBox emailBox;
    @FXML private Region heat;
    @FXML private Label domainName;
    @FXML private Label countBadge;
    @FXML private Button moreButton;
    @FXML private Button unsubscribeButton;

    DomainCard(AppContext context, Account account, MailService mail, String domain,
               Toast notifier, Consumer<List<String>> purge, Runnable onRemoved) {
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
        Fxml.load("domain-card.fxml", this);
    }

    @FXML
    private void initialize() {
        domainName.setText(domain);
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

        // Only senders that actually offer an unsubscribe get the button; the rest are delete-only.
        boolean canUnsubscribe = snapshot.bestUnsubscribe().isPresent();
        unsubscribeButton.setVisible(canUnsubscribe);
        unsubscribeButton.setManaged(canUnsubscribe);

        if (expanded) renderRows();
    }

    // ---- header -------------------------------------------------------------

    @FXML
    private void toggle() {
        expanded = !expanded;
        if (expanded && shown == 0) shown = INITIAL;
        if (expanded) renderRows();
        emailBox.setVisible(expanded);
        emailBox.setManaged(expanded);
    }

    // ---- email rows ---------------------------------------------------------

    @FXML
    private void showMore() {
        int from = Math.min(shown, messages.size());
        shown = Math.min(shown + PAGE, messages.size());
        // Insert the freshly revealed rows before the "Show more" button without removing it, then
        // drop the button once the last row is shown. (The scrollbar is kept steady globally by the
        // dashboard's scroll keeper.)
        int insertAt = emailBox.getChildren().indexOf(moreButton);
        emailBox.getChildren().addAll(insertAt < 0 ? emailBox.getChildren().size() : insertAt, rows(from, shown));
        if (shown >= messages.size()) emailBox.getChildren().remove(moreButton);
    }

    private void renderRows() {
        int visible = Math.min(shown, messages.size());
        emailBox.getChildren().setAll(rows(0, visible));
        if (visible < messages.size()) emailBox.getChildren().add(moreButton);
    }

    private List<Node> rows(int from, int to) {
        return messages.subList(from, to).stream()
                .map(message -> new EmailRow(message, () -> deleteEmail(message)).build())
                .toList();
    }

    // ---- actions ------------------------------------------------------------

    @FXML
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

    @FXML
    private void deleteDomain() {
        delete(messages.stream().map(EmailMessage::id).toList(), onRemoved);
    }

    private void deleteEmail(EmailMessage message) {
        delete(List.of(message.id()), () -> {
            messages.remove(message);
            if (messages.isEmpty()) {
                onRemoved.run();
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
                    notifier.success("Moved " + ids.size() + " email(s) from " + domain + " to Trash.");
                },
                error -> notifier.error("Delete failed: " + Errors.describe(error)));
    }
}
