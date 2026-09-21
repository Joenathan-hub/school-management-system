package com.school.sms.ui;

import com.school.sms.dao.TermDAO;
import com.school.sms.model.Term;
import com.school.sms.model.User;
import com.school.sms.ui.design.Components;
import com.school.sms.ui.design.Icons;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.time.format.DateTimeFormatter;

/**
 * The 58px top bar.
 *
 * <p>Search on the left — the same search the platform-modified-K shortcut opens,
 * presented as a field so it is discoverable by mouse — then, on the right, the
 * active term, notifications and the signed-in user.</p>
 *
 * <p>Everything shown here is real: the term pill names the term the database
 * currently has active (or says that none is set), and the notification dot only
 * appears when there is something to notify about, because a badge that is always
 * lit tells the user nothing.</p>
 */
public final class TopBar {

    private TopBar() {
    }

    public static HBox build(User user, Runnable openSearch) {
        HBox bar = new HBox();
        bar.getStyleClass().add("topbar");
        bar.setAlignment(Pos.CENTER_LEFT);

        bar.getChildren().add(searchField(user, openSearch));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bar.getChildren().add(spacer);

        HBox right = new HBox();
        right.getStyleClass().add("topbar-right");
        right.setAlignment(Pos.CENTER_RIGHT);
        right.getChildren().addAll(termPill(), notificationButton(user), avatar(user));
        bar.getChildren().add(right);
        return bar;
    }

    private static Node searchField(User user, Runnable openSearch) {
        HBox field = Components.searchField("Search students, staff, invoices\u2026", openSearch);
        // The top bar field is a doorway to the palette rather than a second,
        // differently-behaving search box: typing anywhere in it opens the palette.
        for (Node child : field.getChildren()) {
            if (child instanceof javafx.scene.control.TextField input) {
                input.setEditable(false);
                input.setFocusTraversable(true);
                input.setOnKeyPressed(e -> {
                    switch (e.getCode()) {
                        case ENTER, SPACE, DOWN -> {
                            openSearch.run();
                            e.consume();
                        }
                        default -> {
                        }
                    }
                });
                input.setOnMouseClicked(e -> openSearch.run());
            }
        }
        Icons.tooltip(field, "Search everything \u2014 opens the command palette");
        return field;
    }

    /** Names the term the database has active, or says plainly that none is. */
    private static Node termPill() {
        HBox pill = new HBox();
        pill.getStyleClass().add("term-pill");
        pill.setAlignment(Pos.CENTER_LEFT);
        pill.getChildren().add(Icons.boxed(FontAwesomeSolid.CALENDAR_ALT, Icons.INLINE));

        String text = "No active term";
        try {
            Term term = new TermDAO().findActive();
            if (term != null && term.getName() != null) {
                text = term.getName();
                if (term.getEndDate() != null) {
                    text = text + " \u00b7 ends " + term.getEndDate().format(DateTimeFormatter.ofPattern("d MMM yyyy"));
                }
            }
        } catch (Throwable ignored) {
            // An unreadable term table leaves the neutral wording in place.
        }
        pill.getChildren().add(Components.label(text, "term-pill-text"));
        Icons.tooltip(pill, "Active term");
        return pill;
    }

    /**
     * Notifications. The dot is unconditional in the design; here it is driven by
     * whether there is actually something outstanding, and pressing the button says
     * what that is instead of opening an empty panel.
     */
    private static Node notificationButton(User user) {
        Button button = new Button();
        button.getStyleClass().addAll("icon-button", "notif-button");
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setGraphic(Icons.boxed(FontAwesomeSolid.BELL, Icons.INLINE));
        button.setFocusTraversable(true);

        int outstanding;
        try {
            outstanding = new com.school.sms.service.DashboardKpiService().getStudentsNotClearedCount();
        } catch (Throwable ignored) {
            outstanding = 0;
        }

        String tooltip = outstanding == 0
                ? "Nothing needs attention"
                : outstanding + " student(s) not fully cleared";
        Icons.tooltip(button, tooltip);

        final int count = outstanding;
        button.setOnAction(e -> {
            if (count > 0) {
                MainShell.toast(count + " student(s) are not fully cleared. Open the dashboard table for details.",
                        Components.Tone.WARNING);
            } else {
                MainShell.toast("Everything is up to date.", Components.Tone.SUCCESS);
            }
        });

        if (outstanding == 0) {
            return button;
        }
        Region dot = new Region();
        dot.getStyleClass().add("notif-dot");
        StackPane holder = new StackPane(button, dot);
        holder.setAlignment(Pos.CENTER);
        StackPane.setAlignment(dot, Pos.TOP_RIGHT);
        return holder;
    }

    private static Node avatar(User user) {
        StackPane avatar = Components.avatar(user.getFullName());
        Icons.tooltip(avatar, user.getFullName() + " \u00b7 " + (user.getRole() == null ? "" : user.getRole().toString()));
        avatar.setFocusTraversable(false);
        return avatar;
    }
}
