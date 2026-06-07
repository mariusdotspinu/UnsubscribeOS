package com.unsubscribeos.ui.view.dashboard;

import com.unsubscribeos.core.mail.DomainAggregator;
import com.unsubscribeos.core.mail.FetchContext;
import com.unsubscribeos.core.mail.MailService;
import com.unsubscribeos.core.mail.MailServiceFactory;
import com.unsubscribeos.core.model.Account;
import com.unsubscribeos.core.model.EmailMessage;
import com.unsubscribeos.core.model.FetchProgress;
import com.unsubscribeos.core.model.MailDomain;
import com.unsubscribeos.ui.AppContext;
import com.unsubscribeos.ui.Errors;
import com.unsubscribeos.ui.Router;
import com.unsubscribeos.ui.component.AppIcon;
import com.unsubscribeos.ui.component.Modal;
import com.unsubscribeos.ui.component.ProviderLogo;
import com.unsubscribeos.ui.component.ThemeToggle;
import com.unsubscribeos.ui.component.Toast;
import com.unsubscribeos.ui.component.Ui;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main screen: streams the account's mail off-thread behind a live progress bar, then keeps the
 * view fresh by re-polling every few seconds (fetching only messages not already seen). Senders
 * are grouped, searchable and sortable, and volume-graded by colour. Owns the fetch/poll
 * lifecycle and a shared status line; sign-out cancels everything and returns to the providers.
 */
public final class DashboardView {

    private static final Duration RENDER_INTERVAL = Duration.millis(250);
    private static final Duration POLL_INTERVAL = Duration.seconds(5);

    private static final String ABOUT = """
            UnsubscribeOS groups bulk senders by domain so you can unsubscribe from — or delete — \
            them in one calm place.

            • Talks to Gmail and Outlook directly from your computer; your credentials and email never \
            leave this machine.
            • Only marketing / list mail is shown, so personal /primary email stays private and safe from \
            accidental deletion.
            • Deletes move messages to Trash (recoverable); unsubscribe uses the sender's official \
            one-click link when one is offered.

            Free and open source.""";

    private final AppContext context;
    private final Router router;
    private final Account account;
    private final MailService mail;
    private final DomainGrid grid;

    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicBoolean fetching = new AtomicBoolean();
    private final Map<String, EmailMessage> collected = new ConcurrentHashMap<>();

    private final Timeline renderer = new Timeline(new KeyFrame(RENDER_INTERVAL, e -> render()));
    private final Timeline poller = new Timeline(new KeyFrame(POLL_INTERVAL, e -> poll()));

    private final TextField search = new TextField();
    private final ComboBox<DomainSort> sort = new ComboBox<>();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label progressLabel = Ui.label("Connecting to your mailbox…", "muted");
    private final VBox progressBox = new VBox(8, progressLabel, progressBar);
    private final Toast toast = new Toast();
    private StackPane host;

    public DashboardView(AppContext context, Router router, Account account) {
        this.context = context;
        this.router = router;
        this.account = account;
        this.mail = MailServiceFactory.create(account.provider());
        this.grid = new DomainGrid(context, account, mail, toast, this::purge);
    }

    public Parent build() {
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBox.getStyleClass().add("card");
        progressBox.setPadding(new Insets(16));

        VBox content = new VBox(16, progressBox, grid.node());
        content.setPadding(new Insets(20));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);

        BorderPane frame = new BorderPane(scroll);
        frame.getStyleClass().add("root");
        frame.setTop(new VBox(topBar(), toolBar()));
        frame.setBottom(toastBar());

        host = new StackPane(frame);
        renderer.setCycleCount(Timeline.INDEFINITE);
        poller.setCycleCount(Timeline.INDEFINITE);
        startFetch(true);
        return host;
    }

    // ---- toolbar: search + sort --------------------------------------------

    private HBox toolBar() {
        search.setPromptText("Search by domain…");
        search.textProperty().addListener((obs, was, now) -> render());
        HBox.setHgrow(search, Priority.ALWAYS);

        sort.getItems().setAll(DomainSort.values());
        sort.setValue(DomainSort.MOST_EMAILS);
        sort.valueProperty().addListener((obs, was, now) -> render());

        HBox bar = Ui.row(12, Pos.CENTER_LEFT, search, sort);
        bar.setPadding(new Insets(0, 20, 12, 20));
        return bar;
    }

    // ---- fetch + 5s poll ----------------------------------------------------

    private void poll() {
        if (!cancelled.get()) startFetch(false);
    }

    private void startFetch(boolean initial) {
        if (cancelled.get() || !fetching.compareAndSet(false, true)) return;
        renderer.play();
        Thread.ofVirtual().start(() -> {
            try {
                String token = context.authService().accessToken(account);
                mail.fetch(token, new FetchContext(this::onMessage, this::onProgress,
                        cancelled::get, id -> !collected.containsKey(id)));
                Platform.runLater(() -> onFetchDone(initial));
            } catch (Exception e) {
                Platform.runLater(() -> onFetchFailed(e, initial));
            } finally {
                fetching.set(false);
            }
        });
    }

    private void onMessage(EmailMessage message) {
        collected.put(message.id(), message);
    }

    private void onProgress(FetchProgress progress) {
        Platform.runLater(() -> {
            progressBar.setProgress(progress.ratio());
            progressLabel.setText(progress.fetched() + "/" + progress.total() + " emails scanned");
        });
    }

    private void onFetchDone(boolean initial) {
        renderer.stop();
        render();
        if (initial) {
            progressBox.setVisible(false);
            progressBox.setManaged(false);
            poller.play();
        }
    }

    private void onFetchFailed(Throwable error, boolean initial) {
        renderer.stop();
        if (initial) progressLabel.setText("Couldn't fetch mail: " + Errors.describe(error));
        else toast.error("Couldn't refresh: " + Errors.describe(error));
    }

    // ---- rendering: group, search, sort, colour ----------------------------

    private void render() {
        List<MailDomain> all = DomainAggregator.group(new ArrayList<>(collected.values()));
        int max = all.stream().mapToInt(MailDomain::count).max().orElse(0);
        List<MailDomain> view = all.stream()
                .filter(this::matchesSearch)
                .sorted(sort.getValue().comparator())
                .toList();
        if (view.isEmpty()) grid.showMessage(emptyMessage(all.isEmpty()));
        else grid.update(view, max);
    }

    private boolean matchesSearch(MailDomain domain) {
        String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        return query.isEmpty() || domain.domain().toLowerCase().contains(query);
    }

    private String emptyMessage(boolean noMail) {
        if (!noMail) return "No domains match your search.";
        return fetching.get() ? "Scanning your mailbox…" : "No emails found.";
    }

    private void purge(List<String> ids) {
        ids.forEach(collected::remove);
    }

    // ---- chrome -------------------------------------------------------------

    private HBox topBar() {
        HBox provider = Ui.row(8, Pos.CENTER_LEFT,
                ProviderLogo.of(account.provider(), 20),
                Ui.label(account.provider().displayName(), "muted"));
        HBox bar = Ui.row(12, Pos.CENTER_LEFT,
                provider,
                Ui.grow(),
                Ui.icon("ⓘ", "About UnsubscribeOS", this::showAbout, "ghost"),
                ThemeToggle.button(context.themeManager()),
                Ui.button("Sign out", this::signOut, "ghost"));
        bar.setPadding(new Insets(14, 20, 14, 20));
        return bar;
    }

    private void showAbout() {
        Modal.show(host, "About UnsubscribeOS", ABOUT);
    }

    private HBox toastBar() {
        HBox bar = new HBox(toast.node());
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(0, 20, 16, 20));
        bar.setPickOnBounds(false);
        return bar;
    }

    private void signOut() {
        cancelled.set(true);
        poller.stop();
        renderer.stop();
        context.authService().signOut(account.provider());
        router.toProvider();
    }
}
