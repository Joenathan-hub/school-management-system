package com.school.sms.ui;

import com.school.sms.dao.PaymentDAO;
import com.school.sms.dao.StudentDAO;
import com.school.sms.dao.WorkerDAO;
import com.school.sms.model.Payment;
import com.school.sms.model.Student;
import com.school.sms.model.User;
import com.school.sms.model.Worker;
import com.school.sms.ui.design.Components;
import com.school.sms.ui.design.Icons;
import com.school.sms.ui.design.Motion;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * The command palette: one keystroke to anywhere.
 *
 * <p>Opened with the platform modifier plus K. Typing searches students, staff and
 * recorded payments, and also filters the screens the signed-in role can open; a
 * result is activated with Enter, or with the arrow keys and Enter, and the list
 * is a normal focusable list so the whole thing works from the keyboard
 * alone.</p>
 *
 * <p>Database queries run off the UI thread and are debounced, so typing never
 * stutters and a slow Access-backed query cannot freeze the window. Results that
 * arrive out of order are discarded.</p>
 */
public final class CommandPalette {

    /** One searchable result. */
    private record Result(String section, String title, String path, Ikon icon, Runnable action) {
    }

    private final User user;
    private final BiConsumer<Node, String> navigate;
    private final Runnable goHome;

    private final StackPane overlay = new StackPane();
    private final VBox list = new VBox();
    private final TextField input = new TextField();
    private final List<Result> results = new ArrayList<>();
    private final List<HBox> itemNodes = new ArrayList<>();

    private final PauseTransition debounce = new PauseTransition(Duration.millis(180));
    private int selectedIndex = -1;
    private long queryToken;
    private boolean open;

    public CommandPalette(User user, BiConsumer<Node, String> navigate, Runnable goHome) {
        this.user = user;
        this.navigate = navigate;
        this.goHome = goHome;
        build();
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    private void build() {
        overlay.getStyleClass().add("scrim");
        overlay.setAlignment(Pos.TOP_CENTER);
        overlay.setOnMouseClicked(e -> close());

        VBox palette = new VBox();
        palette.getStyleClass().add("command-palette");
        palette.setMaxHeight(Region.USE_PREF_SIZE);
        palette.setOnMouseClicked(e -> e.consume());
        VBox.setMargin(palette, new javafx.geometry.Insets(80, 0, 0, 0));

        HBox field = new HBox();
        field.getStyleClass().add("command-palette-field");
        field.setAlignment(Pos.CENTER_LEFT);
        field.getChildren().add(Icons.boxed(FontAwesomeSolid.SEARCH, Icons.TOPBAR));
        input.setPromptText("Search students, staff, payments \u2014 or jump to a screen");
        input.getStyleClass().add("command-palette-input");
        HBox.setHgrow(input, Priority.ALWAYS);
        field.getChildren().add(input);
        field.getChildren().add(Components.kbdHint("Esc"));

        ScrollPane scroll = new ScrollPane(list);
        scroll.getStyleClass().addAll("content-scroll", "command-palette-list");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        palette.getChildren().addAll(field, scroll);
        overlay.getChildren().add(palette);

        input.textProperty().addListener((obs, old, text) -> {
            debounce.stop();
            debounce.setOnFinished(e -> search(text));
            debounce.play();
        });
        input.setOnKeyPressed(this::handleKey);
        debounce.setOnFinished(e -> search(input.getText()));
    }

    // ------------------------------------------------------------------
    // Open / close
    // ------------------------------------------------------------------

    Node overlay() {
        return overlay;
    }

    boolean isOpen() {
        return open;
    }

    void open() {
        open = true;
        Motion.fadeIn(overlay, Motion.TOAST_IN);
        search("");
        Platform.runLater(input::requestFocus);
    }

    void close() {
        open = false;
        if (overlay.getParent() instanceof StackPane parent) {
            parent.getChildren().remove(overlay);
        }
    }

    void focus() {
        input.requestFocus();
    }

    // ------------------------------------------------------------------
    // Searching
    // ------------------------------------------------------------------

    private void search(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        long token = ++queryToken;

        // Screen navigation is instant - it is already in memory.
        render(new ArrayList<>(navResults(query)));

        if (query.isEmpty()) {
            return;
        }

        Thread worker = new Thread(() -> {
            List<Result> found = databaseResults(query);
            Platform.runLater(() -> {
                if (token != queryToken) {
                    return; // a newer keystroke has already superseded this search
                }
                List<Result> combined = new ArrayList<>(found);
                combined.addAll(navResults(query));
                render(combined);
            });
        }, "command-palette-search");
        worker.setDaemon(true);
        worker.start();
    }

    private List<Result> databaseResults(String query) {
        List<Result> found = new ArrayList<>();
        String needle = query.toLowerCase(Locale.ROOT);

        try {
            List<Student> students = new StudentDAO().search(query);
            if (students == null) {
                students = new StudentDAO().findAll();
            }
            int shown = 0;
            for (Student student : students) {
                if (shown++ >= 5) {
                    break;
                }
                if (student.getFullName() == null || !student.getFullName().toLowerCase(Locale.ROOT).contains(needle)) {
                    continue;
                }
                found.add(new Result("Students", student.getFullName(),
                        "Student \u00b7 " + text(student.getStudentClass()) + " \u00b7 " + text(student.getStudentId()),
                        FontAwesomeSolid.USER_GRADUATE,
                        () -> StudentsListScreen.show(MainShell.getStage(), user)));
            }
        } catch (Throwable ignored) {
            // A database that cannot be reached simply yields no rows.
        }

        try {
            List<Worker> workers = new WorkerDAO().search(query);
            if (workers == null) {
                workers = new WorkerDAO().findAll();
            }
            int shown = 0;
            for (Worker worker : workers) {
                if (shown++ >= 5) {
                    break;
                }
                String name = worker.getFullName();
                if (name == null || !name.toLowerCase(Locale.ROOT).contains(needle)) {
                    continue;
                }
                found.add(new Result("Staff", name,
                        "Staff \u00b7 " + text(worker.getJobTitle()) + " \u00b7 " + text(worker.getWorkerId()),
                        FontAwesomeSolid.USER_TIE,
                        () -> WorkerScreen.show(MainShell.getStage(), user)));
            }
        } catch (Throwable ignored) {
            // Ignored for the same reason as above.
        }

        try {
            for (Payment payment : new PaymentDAO().findAll()) {
                String receipt = String.valueOf(payment.getReceiptNumber());
                if (!receipt.contains(needle)) {
                    continue;
                }
                found.add(new Result("Payments", "Receipt #" + receipt,
                        "Payment \u00b7 " + Components.money(payment.getAmount()),
                        FontAwesomeSolid.RECEIPT,
                        () -> PaymentScreen.show(MainShell.getStage(), user)));
                if (found.size() > 14) {
                    break;
                }
            }
        } catch (Throwable ignored) {
            // Ignored for the same reason as above.
        }

        return found;
    }

    /** Jumps to the screens this role can actually open. */
    private List<Result> navResults(String query) {
        List<Result> found = new ArrayList<>();
        String needle = query == null ? "" : query.toLowerCase(Locale.ROOT);
        for (Result jump : jumps()) {
            if (needle.isEmpty() || jump.title().toLowerCase(Locale.ROOT).contains(needle)) {
                found.add(jump);
            }
        }
        return found;
    }

    private List<Result> jumps() {
        List<Result> jumps = new ArrayList<>();
        jumps.add(new Result("Go to", "Dashboard", "Overview", FontAwesomeSolid.HOME, goHome));
        boolean finance = user.getRole() == com.school.sms.model.Role.ADMINISTRATOR
                || user.getRole() == com.school.sms.model.Role.BURSAR;

        jumps.add(new Result("Go to", "Students List", "Students", FontAwesomeSolid.USERS,
                () -> StudentsListScreen.show(MainShell.getStage(), user)));
        jumps.add(new Result("Go to", "Admit New Student", "Students", FontAwesomeSolid.USER_GRADUATE,
                () -> AdmissionFormScreen.show(MainShell.getStage(), user)));

        if (finance) {
            jumps.add(new Result("Go to", "Record Student Payment", "Finance", FontAwesomeSolid.MONEY_BILL_WAVE,
                    () -> PaymentScreen.show(MainShell.getStage(), user)));
            jumps.add(new Result("Go to", "Treasury Dashboard", "Finance", FontAwesomeSolid.CHART_LINE,
                    () -> TreasuryScreen.show(MainShell.getStage(), user)));
            jumps.add(new Result("Go to", "Report Cards", "Academics", FontAwesomeSolid.FILE_ALT,
                    () -> ReportCardScreen.show(MainShell.getStage(), user)));
            jumps.add(new Result("Go to", "Requirements Checklist", "Students", FontAwesomeSolid.CLIPBOARD_CHECK,
                    () -> RequirementsScreen.show(MainShell.getStage(), user)));
            jumps.add(new Result("Go to", "Uniform Checklist", "Students", FontAwesomeSolid.TSHIRT,
                    () -> UniformScreen.show(MainShell.getStage(), user)));
            jumps.add(new Result("Go to", "Search Students", "Students", FontAwesomeSolid.SEARCH,
                    () -> AdvancedSearchScreen.show(MainShell.getStage(), user)));
        }
        if (user.getRole() == com.school.sms.model.Role.ADMINISTRATOR) {
            jumps.add(new Result("Go to", "User Accounts", "Administration", FontAwesomeSolid.USER_COG,
                    () -> UserManagementScreen.show(MainShell.getStage(), user)));
            jumps.add(new Result("Go to", "Term Management", "Academics", FontAwesomeSolid.CALENDAR_ALT,
                    () -> TermManagementScreen.show(MainShell.getStage(), user)));
            jumps.add(new Result("Go to", "Audit Log", "Administration", FontAwesomeSolid.HISTORY,
                    () -> AuditLogScreen.show(MainShell.getStage(), user)));
            jumps.add(new Result("Go to", "Backup Database", "Administration", FontAwesomeSolid.DATABASE,
                    MainShell::backupDatabase));
        }
        if (user.getRole() == com.school.sms.model.Role.TEACHER) {
            jumps.add(new Result("Go to", "Enter Results", "Classroom", FontAwesomeSolid.PEN,
                    () -> ResultsEntryScreen.show(MainShell.getStage(), user)));
            jumps.add(new Result("Go to", "Attendance", "Classroom", FontAwesomeSolid.CLIPBOARD_LIST,
                    () -> AttendanceScreen.show(MainShell.getStage(), user)));
        }
        return jumps;
    }

    // ------------------------------------------------------------------
    // Rendering and keyboard
    // ------------------------------------------------------------------

    private void render(List<Result> newResults) {
        results.clear();
        results.addAll(newResults);
        itemNodes.clear();
        list.getChildren().clear();
        selectedIndex = results.isEmpty() ? -1 : 0;

        if (results.isEmpty()) {
            Label empty = Components.label("Nothing matches that search yet.", "command-empty");
            empty.setWrapText(true);
            list.getChildren().add(empty);
            return;
        }

        String section = null;
        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            if (!result.section().equals(section)) {
                section = result.section();
                list.getChildren().add(Components.label(section.toUpperCase(Locale.ROOT), "command-section"));
            }
            list.getChildren().add(itemNode(result, i));
        }
        highlight(0);
    }

    private HBox itemNode(Result result, int index) {
        HBox item = new HBox();
        item.getStyleClass().add("command-item");
        item.setAlignment(Pos.CENTER_LEFT);
        item.getChildren().add(Icons.boxed(result.icon(), Icons.TOPBAR));
        VBox text = new VBox(1, Components.label(result.title(), "command-item-title"),
                Components.label(result.path(), "command-item-path"));
        HBox.setHgrow(text, Priority.ALWAYS);
        item.getChildren().add(text);
        item.setOnMouseClicked(e -> activate(result));
        item.setOnMouseEntered(e -> highlight(index));
        itemNodes.add(item);
        return item;
    }

    private void highlight(int index) {
        if (index < 0 || index >= itemNodes.size()) {
            return;
        }
        selectedIndex = index;
        for (int i = 0; i < itemNodes.size(); i++) {
            HBox item = itemNodes.get(i);
            item.getStyleClass().remove("command-item-selected");
            if (i == index) {
                item.getStyleClass().add("command-item-selected");
            }
        }
    }

    private void activate(Result result) {
        close();
        if (result == null) {
            return;
        }
        Runnable action = result.action();
        if (action != null) {
            action.run();
        }
    }

    private void handleKey(javafx.scene.input.KeyEvent event) {
        switch (event.getCode()) {
            case DOWN -> {
                highlight(Math.min(results.size() - 1, selectedIndex + 1));
                event.consume();
            }
            case UP -> {
                highlight(Math.max(0, selectedIndex - 1));
                event.consume();
            }
            case ENTER -> {
                if (selectedIndex >= 0 && selectedIndex < results.size()) {
                    activate(results.get(selectedIndex));
                }
                event.consume();
            }
            case ESCAPE -> {
                close();
                event.consume();
            }
            default -> {
            }
        }
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? "\u2014" : value;
    }
}
