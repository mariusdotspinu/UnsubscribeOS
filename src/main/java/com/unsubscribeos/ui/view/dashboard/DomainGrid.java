package com.unsubscribeos.ui.view.dashboard;

import com.unsubscribeos.core.mail.MailService;
import com.unsubscribeos.core.model.Account;
import com.unsubscribeos.core.model.MailDomain;
import com.unsubscribeos.ui.AppContext;
import com.unsubscribeos.ui.component.Notifier;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Controller for {@code domain-grid.fxml}: the live list of sender cards. Reuses a {@link DomainCard} per domain across refreshes so each
 * update only re-orders existing nodes (busiest first) and refreshes their contents — preserving
 * expand state and avoiding a full rebuild on every fetch tick.
 */
final class DomainGrid {

    private final AppContext context;
    private final Account account;
    private final MailService mail;
    private final Notifier notifier;
    private final Consumer<List<String>> purge;

    @FXML private VBox list;
    @FXML private Label message;

    private final Map<String, DomainCard> cards = new HashMap<>();

    DomainGrid(AppContext context, Account account, MailService mail,
               Notifier notifier, Consumer<List<String>> purge) {
        this.context = context;
        this.account = account;
        this.mail = mail;
        this.notifier = notifier;
        this.purge = purge;
    }

    void update(List<MailDomain> domains, int maxCount) {
        List<Node> ordered = new ArrayList<>();
        for (MailDomain domain : domains) {
            DomainCard card = cards.computeIfAbsent(domain.domain(), this::create);
            card.update(domain, maxCount);
            ordered.add(card.node());
        }
        list.getChildren().setAll(ordered);
    }

    void showMessage(String text) {
        message.setText(text);
        list.getChildren().setAll(message);
    }

    private DomainCard create(String domain) {
        DomainCard card = new DomainCard(context, account, mail, domain, notifier, purge, () -> remove(domain));
        card.build();
        return card;
    }

    private void remove(String domain) {
        DomainCard card = cards.remove(domain);
        if (card != null) list.getChildren().remove(card.node());
    }
}
