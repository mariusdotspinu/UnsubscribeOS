package com.unsubscribeos.ui.view.dashboard;

import com.unsubscribeos.core.model.EmailMessage;
import com.unsubscribeos.ui.Fxml;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;

/** Controller for {@code email-row.fxml}: one email inside an expanded {@link DomainCard}. */
final class EmailRow {

    private final EmailMessage message;
    private final Runnable onDelete;

    @FXML private Label subject;
    @FXML private Label snippet;

    EmailRow(EmailMessage message, Runnable onDelete) {
        this.message = message;
        this.onDelete = onDelete;
    }

    Node build() {
        return Fxml.load("email-row.fxml", this);
    }

    @FXML
    private void initialize() {
        subject.setText(message.subject());
        snippet.setText(message.snippet());
    }

    @FXML
    private void delete() {
        onDelete.run();
    }
}
