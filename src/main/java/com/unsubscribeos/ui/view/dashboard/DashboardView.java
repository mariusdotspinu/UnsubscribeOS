package com.unsubscribeos.ui.view.dashboard;

import com.unsubscribeos.config.Settings;
import com.unsubscribeos.core.mail.FetchContext;
import com.unsubscribeos.core.mail.MailService;
import com.unsubscribeos.core.model.Account;
import com.unsubscribeos.core.model.EmailMessage;
import com.unsubscribeos.core.model.FetchProgress;
import com.unsubscribeos.core.model.MailDomain;
import com.unsubscribeos.ui.AppContext;
import com.unsubscribeos.ui.Async;
import com.unsubscribeos.ui.Errors;
import com.unsubscribeos.ui.Router;
import com.unsubscribeos.ui.component.Modal;
import com.unsubscribeos.ui.component.ProviderLogo;
import com.unsubscribeos.ui.component.Toast;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public final class DashboardView {

    private static final Duration RENDER_INTERVAL = Duration.millis(250);
    private static final Duration POLL_INTERVAL = Duration.seconds(5);

    // Polls only need to notice new mail, so they re-list just the newest few hundred.
    private static final int POLL_DEPTH = 500;

    private static final String ABOUT = """
            UnsubscribeOS groups the mail you've received by sender domain so you can unsubscribe \
            from — or delete — senders in one calm place.

            • Talks to Gmail and Outlook directly from your computer; your credentials and email never \
            leave this machine.
            • Shows every sender who mails you, busiest first. Senders that offer an unsubscribe get an \
            unsubscribe button; any sender can be deleted.
            • Deletes move messages to Trash (recoverable); unsubscribe uses the sender's official \
            one-click link when one is offered.

            Free and open source.""";

    private final AppContext context;
    private final Router router;
    private final Account account;
    private final MailService mail;

    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicBoolean fetching = new AtomicBoolean();
    private final AtomicBoolean pendingRescan = new AtomicBoolean();
    // Deleted ids stay here so a straggling poll can't resurrect them.
    private final Set<String> seen = ConcurrentHashMap.newKeySet();
    private final Map<String, EmailMessage> collected = new ConcurrentHashMap<>();

    private final Timeline renderer = new Timeline(new KeyFrame(RENDER_INTERVAL, e -> render()));
    private final Timeline poller = new Timeline(new KeyFrame(POLL_INTERVAL, e -> poll()));

    private final Map<String, DomainCard> cards = new HashMap<>();

    @FXML private StackPane host;
    @FXML private StackPane providerLogo;
    @FXML private Label providerName;
    @FXML private TextField search;
    @FXML private ComboBox<DomainSort> sort;
    @FXML private ComboBox<Integer> depth;
    @FXML private ScrollPane scroll;
    @FXML private VBox progressBox;
    @FXML private Label progressLabel;
    @FXML private ProgressBar progressBar;
    @FXML private VBox list;
    @FXML private Label message;
    @FXML private Toast toastController;

    private double desiredScroll;
    private boolean userScrolling;

    public DashboardView(AppContext context, Router router, Account account) {
        this.context = context;
        this.router = router;
        this.account = account;
        this.mail = MailService.of(account.provider());
    }

    /** Keeps the user's scroll position: content rebuilds snap it to the top, so non-user changes are undone. */
    private void installScrollKeeper() {
        scroll.addEventFilter(ScrollEvent.SCROLL, e -> markUserScroll());
        scroll.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> { if (onScrollBar(e.getTarget())) markUserScroll(); });
        scroll.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> { if (onScrollBar(e.getTarget())) markUserScroll(); });
        scroll.addEventFilter(KeyEvent.KEY_PRESSED, e -> { if (isScrollKey(e.getCode())) markUserScroll(); });
        scroll.vvalueProperty().addListener((obs, was, now) -> {
            if (userScrolling) desiredScroll = now.doubleValue();
            else if (now.doubleValue() != desiredScroll) scroll.setVvalue(desiredScroll);
        });
    }

    private static boolean isScrollKey(KeyCode code) {
        return switch (code) {
            case UP, DOWN, PAGE_UP, PAGE_DOWN, HOME, END -> true;
            default -> false;
        };
    }

    private void markUserScroll() {
        userScrolling = true;
        Platform.runLater(() -> userScrolling = false);
    }

    private static boolean onScrollBar(EventTarget target) {
        Node node = target instanceof Node n ? n : null;
        for (; node != null; node = node.getParent()) {
            if (node instanceof ScrollBar) return true;
        }
        return false;
    }

    @FXML
    private void initialize() {
        providerLogo.getChildren().setAll(ProviderLogo.of(account.provider(), 20));
        providerName.setText(account.provider().displayName());
        installScrollKeeper();
        initToolBar();

        renderer.setCycleCount(Timeline.INDEFINITE);
        poller.setCycleCount(Timeline.INDEFINITE);
        startFetch(true);
    }

    private void initToolBar() {
        search.textProperty().addListener((obs, was, now) -> render());

        sort.getItems().setAll(DomainSort.values());
        sort.setValue(DomainSort.MOST_EMAILS);
        sort.valueProperty().addListener((obs, was, now) -> render());

        initDepthSelector();
    }

    /** The listener is added last so setting the initial value doesn't trigger a re-scan. */
    private void initDepthSelector() {
        int current = context.settings().scanDepth();
        depth.getItems().setAll(Stream.of(1000, 5000, 15000, 50000, current).distinct().sorted().toList());
        depth.setValue(current);
        depth.setConverter(new StringConverter<>() {
            @Override public String toString(Integer n) { return n == null ? "" : String.format("Scan %,d", n); }
            @Override public Integer fromString(String s) { return depth.getValue(); }
        });
        depth.valueProperty().addListener((obs, was, now) -> changeDepth(now));
    }

    private void poll() {
        if (!cancelled.get()) startFetch(false);
    }

    private void startFetch(boolean initial) {
        if (cancelled.get() || !fetching.compareAndSet(false, true)) return;
        if (initial) showProgress();
        renderer.play();
        int scanDepth = initial ? context.settings().scanDepth() : POLL_DEPTH;
        Async.run(() -> mail.fetch(context.authService().accessToken(account), scanDepth,
                        new FetchContext(this::onMessage, this::onProgress, cancelled::get, id -> !seen.contains(id))),
                () -> onFetchDone(initial),
                error -> onFetchFailed(error, initial));
    }

    private void changeDepth(Integer value) {
        if (value == null) return;
        context.settings().put(Settings.SCAN_DEPTH, String.valueOf(value));
        pendingRescan.set(true);
        drainRescan();
    }

    /** Also called when any fetch completes, so a re-scan requested mid-poll still runs. */
    private void drainRescan() {
        if (cancelled.get() || fetching.get()) return;
        if (pendingRescan.compareAndSet(true, false)) startFetch(true);
    }

    private void showProgress() {
        progressBar.setProgress(0);
        progressLabel.setText("Scanning your mailbox…");
        progressBox.setVisible(true);
        progressBox.setManaged(true);
    }

    private void onMessage(EmailMessage message) {
        seen.add(message.id());
        collected.put(message.id(), message);
    }

    private void onProgress(FetchProgress progress) {
        Platform.runLater(() -> {
            progressBar.setProgress(progress.ratio());
            progressLabel.setText(progress.fetched() + "/" + progress.total() + " emails scanned");
        });
    }

    private void onFetchDone(boolean initial) {
        fetching.set(false);
        renderer.stop();
        render();
        if (initial) {
            hideProgress();
            poller.play();
        }
        drainRescan();
    }

    private void onFetchFailed(Throwable error, boolean initial) {
        fetching.set(false);
        renderer.stop();
        if (initial) hideProgress();
        render();
        String reason = "Couldn't fetch mail: " + Errors.describe(error);
        if (collected.isEmpty()) showMessage(reason);
        else toastController.error(reason);
        drainRescan();
    }

    private void hideProgress() {
        progressBox.setVisible(false);
        progressBox.setManaged(false);
    }

    private void render() {
        List<MailDomain> all = MailDomain.group(collected.values());
        int max = all.stream().mapToInt(MailDomain::count).max().orElse(0);
        List<MailDomain> view = all.stream()
                .filter(this::matchesSearch)
                .sorted(sort.getValue().comparator())
                .toList();
        if (view.isEmpty()) showMessage(emptyMessage(all.isEmpty()));
        else showCards(view, max);
    }

    private void showCards(List<MailDomain> domains, int maxCount) {
        list.getChildren().setAll(domains.stream().map(domain -> {
            DomainCard card = cards.computeIfAbsent(domain.domain(), this::createCard);
            card.update(domain, maxCount);
            return card.node();
        }).toList());
    }

    private void showMessage(String text) {
        message.setText(text);
        list.getChildren().setAll(message);
    }

    private DomainCard createCard(String domain) {
        DomainCard card = new DomainCard(context, account, mail, domain, toastController, this::purge,
                () -> removeCard(domain));
        card.build();
        return card;
    }

    private void removeCard(String domain) {
        DomainCard card = cards.remove(domain);
        if (card != null) list.getChildren().remove(card.node());
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

    @FXML
    private void showAbout() {
        Modal.show(host, "About UnsubscribeOS", ABOUT);
    }

    @FXML
    private void signOut() {
        cancelled.set(true);
        poller.stop();
        renderer.stop();
        context.authService().signOut(account.provider());
        router.toProvider();
    }
}
