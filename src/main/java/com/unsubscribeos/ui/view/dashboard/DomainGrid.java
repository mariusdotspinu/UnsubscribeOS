package com.unsubscribeos.ui.view.dashboard;

import com.unsubscribeos.core.mail.MailService;
import com.unsubscribeos.core.model.Account;
import com.unsubscribeos.core.model.MailDomain;
import com.unsubscribeos.ui.AppContext;
import com.unsubscribeos.ui.component.Notifier;
import com.unsubscribeos.ui.component.Ui;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The live list of sender cards. Reuses a {@link DomainCard} per domain across refreshes so each
 * update only re-orders existing nodes (busiest first) and refreshes their contents — preserving
 * expand state and avoiding a full rebuild on every fetch tick.
 */
final class DomainGrid {

    private final AppContext context;
    private final Account account;
    private final MailService mail;
    private final Notifier notifier;
    private final Consumer<List<String>> purge;

    private final VBox list = new VBox(12);
    private final Map<String, DomainCard> cards = new HashMap<>();

    DomainGrid(AppContext context, Account account, MailService mail,
               Notifier notifier, Consumer<List<String>> purge) {
        this.context = context;
        this.account = account;
        this.mail = mail;
        this.notifier = notifier;
        this.purge = purge;
    }

    Node node() {
        return list;
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

    void showMessage(String message) {
        list.getChildren().setAll(Ui.label(message, "muted"));
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
