package com.school.sms.ui;

import com.school.sms.dao.SettingsDAO;
import com.school.sms.model.Role;
import com.school.sms.model.User;
import com.school.sms.ui.design.Components;
import com.school.sms.ui.design.Icons;
import com.school.sms.ui.design.Motion;
import com.school.sms.util.ThemeManager;
import com.school.sms.util.ThemeMode;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The navigation rail.
 *
 * <p>The rail is dark in both themes by design: in light mode its weight is what
 * gives the interface its institutional character, and it is deliberately never
 * lightened. Its colours are tokens like everything else, so there is no
 * per-mode logic here — the two token sheets simply supply different values.</p>
 *
 * <h2>Role-filtered navigation</h2>
 * Each role is given its own tree. A tree only ever contains what the role can
 * actually open: items a role may not use are not rendered at all — never
 * rendered and disabled, which would advertise a capability the user does not
 * have and add keyboard stops that lead nowhere.</p>
 *
 * <h2>Collapse</h2>
 * Collapsing hides labels, section headings and the wordmark — it does not
 * truncate them to an ellipsis — centres the icons and gives every item a
 * tooltip, because an icon on its own does not say what it does. The width
 * transition is 220 ms and interruptible: pressing the toggle again mid-flight
 * reverses from wherever the rail currently is. The state is stored as a
 * preference, so it survives a restart.</p>
 */
public final class RailNav {

    /** Expanded and collapsed widths from the design. */
    private static final double EXPANDED_WIDTH = 236;
    private static final double COLLAPSED_WIDTH = 68;
    private static final double CONTENT_SPACING = 10;

    private static final Object WIDTH_ANIMATION_KEY = new Object();

    /** One navigable entry in a role's tree. */
    private record Entry(String section, String label, Ikon icon, Runnable action, Integer count) {
    }

    private final User user;
    private final VBox rail = new VBox();
    private final VBox navBox = new VBox();
    private final VBox footer = new VBox();
    private final DoubleProperty width = new SimpleDoubleProperty(EXPANDED_WIDTH);

    private final List<Button> itemButtons = new ArrayList<>();
    private final List<HBox> itemContents = new ArrayList<>();
    private final List<Node> hideWhenCollapsed = new ArrayList<>();
    private final Map<Button, Tooltip> tooltips = new HashMap<>();

    private Button collapseButton;
    private Label collapseLabel;
    private StackPane collapseIconSlot;
    private Label themeLabel;
    private StackPane themeIconSlot;

    private boolean collapsed;
    private String activeLabel;

    public RailNav(User user) {
        this.user = user;
        build();
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    private void build() {
        rail.getStyleClass().add("rail");
        rail.setFillWidth(true);
        // The rail's width is the one dimension JavaFX CSS cannot animate, so it is
        // owned here rather than in the stylesheet: one property, one owner.
        rail.minWidthProperty().bind(width);
        rail.prefWidthProperty().bind(width);
        rail.maxWidthProperty().bind(width);
        width.set(EXPANDED_WIDTH);

        rail.getChildren().add(brand());

        ScrollPane scroll = new ScrollPane(navBox);
        scroll.getStyleClass().add("content-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        rail.getChildren().add(scroll);

        buildNav();
        rail.getChildren().add(footer());

        ThemeManager.addListener(this::refreshThemeControl);

        collapsed = ThemeManager.isRailCollapsed();
        width.set(collapsed ? COLLAPSED_WIDTH : EXPANDED_WIDTH);
        syncCollapseState();
    }

    private Node brand() {
        StackPane tile = new StackPane();
        tile.getStyleClass().add("rail-tile");
        tile.getChildren().add(Components.label(schoolInitials(), "rail-tile-text"));
        tile.setMinSize(34, 34);
        tile.setPrefSize(34, 34);
        tile.setMaxSize(34, 34);

        Label wordmark = Components.label(schoolName(), "rail-wordmark");
        wordmark.setMaxWidth(150);
        wordmark.setTextOverrun(OverrunStyle.ELLIPSIS);
        Icons.tooltip(wordmark, schoolName());
        Label subtitle = Components.label("MANAGEMENT SUITE", "rail-subtitle");

        VBox text = new VBox();
        text.getStyleClass().add("rail-brand-text");
        text.getChildren().addAll(wordmark, subtitle);
        hideWhenCollapsed.add(text);

        HBox brand = new HBox();
        brand.getStyleClass().add("rail-brand");
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getChildren().addAll(tile, text);
        return brand;
    }

    private String schoolName() {
        try {
            String name = new SettingsDAO().get("school.name", "School Management System");
            return name == null || name.isBlank() ? "School Management System" : name;
        } catch (Throwable ignored) {
            return "School Management System";
        }
    }

    private String schoolInitials() {
        String[] parts = schoolName().trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty() && initials.length() < 2 && Character.isLetter(part.charAt(0))) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return initials.length() == 0 ? "SMS" : initials.toString();
    }

    private void buildNav() {
        navBox.getStyleClass().add("rail-nav");
        String currentSection = null;
        VBox sectionBox = null;

        for (Entry entry : entriesFor(user)) {
            if (!entry.section().equals(currentSection)) {
                currentSection = entry.section();
                sectionBox = new VBox();
                sectionBox.getStyleClass().add("rail-section");
                if (!entry.section().isEmpty()) {
                    Label sectionLabel = Components.label(entry.section().toUpperCase(), "rail-section-label");
                    sectionBox.getChildren().add(sectionLabel);
                    hideWhenCollapsed.add(sectionLabel);
                }
                navBox.getChildren().add(sectionBox);
            }
            if (sectionBox != null) {
                sectionBox.getChildren().add(navItem(entry));
            }
        }
    }

    private Button navItem(Entry entry) {
        Region indicator = new Region();
        indicator.getStyleClass().add("rail-item-spacer");

        Node icon = Icons.boxed(entry.icon(), Icons.NAV, "rail-item-icon");
        Label label = Components.label(entry.label(), "rail-item-label");
        hideWhenCollapsed.add(label);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox content = new HBox();
        content.getStyleClass().add("rail-item-content");
        content.setAlignment(Pos.CENTER_LEFT);
        content.setSpacing(CONTENT_SPACING);
        content.getChildren().addAll(indicator, icon, label, spacer);

        if (entry.count() != null) {
            Label count = Components.label(Components.formatCount(entry.count()), "rail-count");
            content.getChildren().add(count);
            hideWhenCollapsed.add(count);
        }

        Button item = new Button();
        item.getStyleClass().add("rail-item");
        item.setGraphic(content);
        item.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        item.setMaxWidth(Double.MAX_VALUE);
        item.setUserData(entry.label());
        item.setOnAction(e -> {
            setActive(entry.label());
            entry.action().run();
        });

        itemButtons.add(item);
        itemContents.add(content);
        return item;
    }

    /** Collapse toggle, signed-in user, theme control and log out. */
    private Node footer() {
        footer.getStyleClass().add("rail-footer");
        footer.setFillWidth(true);
        VBox.setVgrow(footer, Priority.NEVER);

        footer.getChildren().add(collapseControl());

        Region divider = new Region();
        divider.getStyleClass().add("rail-divider");
        footer.getChildren().add(divider);

        footer.getChildren().add(userChip());
        footer.getChildren().add(themeControl());

        Button logout = new Button();
        logout.getStyleClass().addAll("rail-item", "rail-logout");
        Region logoutBar = new Region();
        logoutBar.getStyleClass().add("rail-item-spacer");
        Label logoutLabel = Components.label("Log Out", "rail-item-label");
        hideWhenCollapsed.add(logoutLabel);
        HBox logoutContent = new HBox();
        logoutContent.setAlignment(Pos.CENTER_LEFT);
        logoutContent.setSpacing(CONTENT_SPACING);
        logoutContent.getChildren().addAll(logoutBar,
                Icons.boxed(FontAwesomeSolid.SIGN_OUT_ALT, Icons.NAV, "rail-item-icon"), logoutLabel);
        logout.setGraphic(logoutContent);
        logout.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setUserData("Log Out");
        logout.setOnAction(e -> {
            MainShell.reset();
            com.school.sms.Main.showLogin(MainShell.getStage());
        });
        itemButtons.add(logout);
        itemContents.add(logoutContent);
        footer.getChildren().add(logout);
        return footer;
    }

    private Button collapseControl() {
        collapseButton = new Button();
        collapseButton.getStyleClass().add("rail-collapse-button");
        collapseButton.setMaxWidth(Double.MAX_VALUE);
        collapseButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        Region bar = new Region();
        bar.getStyleClass().add("rail-item-spacer");
        collapseIconSlot = new StackPane();
        collapseIconSlot.getStyleClass().add("icon-slot");
        collapseIconSlot.setMinSize(Icons.NAV, Icons.NAV);
        collapseIconSlot.setPrefSize(Icons.NAV, Icons.NAV);
        collapseIconSlot.setMaxSize(Icons.NAV, Icons.NAV);
        collapseLabel = Components.label("Collapse", "rail-item-label");
        hideWhenCollapsed.add(collapseLabel);

        HBox content = new HBox();
        content.setAlignment(Pos.CENTER_LEFT);
        content.setSpacing(CONTENT_SPACING);
        content.getChildren().addAll(bar, collapseIconSlot, collapseLabel);
        collapseButton.setGraphic(content);
        collapseButton.setOnAction(e -> toggleCollapsed());
        itemContents.add(content);
        return collapseButton;
    }

    private Node userChip() {
        StackPane avatar = Components.avatar(user.getFullName());
        Label name = Components.label(user.getFullName(), "rail-user-name");
        Label role = Components.label(roleText(), "rail-user-role");
        VBox text = new VBox(name, role);
        text.setSpacing(1);
        hideWhenCollapsed.add(text);

        HBox chip = new HBox(CONTENT_SPACING);
        chip.getStyleClass().add("rail-user");
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getChildren().addAll(avatar, text);
        Icons.tooltip(chip, user.getFullName() + " \u00b7 " + roleText());
        itemContents.add(chip);
        return chip;
    }

    private String roleText() {
        String role = user.getRole() == null ? "" : user.getRole().toString();
        String assigned = user.getAssignedClass();
        return assigned == null || assigned.isBlank() ? role : role + " \u00b7 " + assigned;
    }

    /**
     * The three-state theme control. Expanded it is a labelled segmented control;
     * collapsed it becomes a single button that cycles the same three states, so
     * the choice is never trapped behind an expanded rail.
     */
    private Node themeControl() {
        StackPane holder = new StackPane();
        holder.setAlignment(Pos.CENTER_LEFT);

        HBox segmented = Components.segmented(
                List.of(ThemeMode.LIGHT.label(), ThemeMode.DARK.label(), ThemeMode.SYSTEM.label()),
                indexOf(ThemeManager.getMode()),
                index -> ThemeManager.setMode(ThemeMode.values()[index]));
        segmented.getStyleClass().add("rail-theme-segmented");
        hideWhenCollapsed.add(segmented);

        Button cycle = new Button();
        cycle.getStyleClass().add("rail-collapse-button");
        cycle.setMaxWidth(Double.MAX_VALUE);
        cycle.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        Region bar = new Region();
        bar.getStyleClass().add("rail-item-spacer");
        themeIconSlot = new StackPane();
        themeIconSlot.getStyleClass().add("icon-slot");
        themeIconSlot.setMinSize(Icons.NAV, Icons.NAV);
        themeIconSlot.setPrefSize(Icons.NAV, Icons.NAV);
        themeIconSlot.setMaxSize(Icons.NAV, Icons.NAV);
        themeLabel = Components.label("", "rail-item-label");
        hideWhenCollapsed.add(themeLabel);
        HBox cycleContent = new HBox();
        cycleContent.setAlignment(Pos.CENTER_LEFT);
        cycleContent.setSpacing(CONTENT_SPACING);
        cycleContent.getChildren().addAll(bar, themeIconSlot, themeLabel);
        cycle.setGraphic(cycleContent);
        cycle.setOnAction(e -> ThemeManager.cycleMode());
        itemContents.add(cycleContent);
        hideWhenCollapsed.add(cycle);

        holder.getChildren().addAll(segmented, cycle);
        refreshThemeControl();
        return holder;
    }

    /** Keeps the collapsed theme control's glyph and wording in step with the mode. */
    private void refreshThemeControl() {
        if (themeLabel == null || themeIconSlot == null) {
            return;
        }
        themeLabel.setText("Theme: " + ThemeManager.getMode().label());
        Ikon icon = switch (ThemeManager.getMode()) {
            case LIGHT -> FontAwesomeSolid.SUN;
            case DARK -> FontAwesomeSolid.MOON;
            case SYSTEM -> FontAwesomeSolid.ADJUST;
        };
        renderIconInto(themeIconSlot, icon);
    }

    private void renderIconInto(StackPane slot, Ikon icon) {
        Node glyph = Icons.of(icon, Icons.NAV, "rail-item-icon");
        slot.getChildren().setAll(glyph);
    }

    private static int indexOf(ThemeMode mode) {
        ThemeMode[] values = ThemeMode.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i] == mode) {
                return i;
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------
    // Collapse
    // ------------------------------------------------------------------

    /** Toggles the rail: width animates, contents switch immediately. */
    public void toggleCollapsed() {
        setCollapsed(!collapsed, true);
    }

    public void setCollapsed(boolean collapse, boolean animate) {
        collapsed = collapse;
        ThemeManager.setRailCollapsed(collapse);
        if (collapse) {
            if (!rail.getStyleClass().contains("rail-collapsed")) {
                rail.getStyleClass().add("rail-collapsed");
            }
        } else {
            rail.getStyleClass().remove("rail-collapsed");
        }
        syncCollapseState();

        double target = collapse ? COLLAPSED_WIDTH : EXPANDED_WIDTH;
        if (animate) {
            Motion.animateWidth(WIDTH_ANIMATION_KEY, width, target, Motion.RAIL);
        } else {
            width.set(target);
        }
    }

    /**
     * Applies the collapsed look: hidden labels, centred icons, tooltips on
     * everything that is now icon-only, and the toggle's own wording and glyph.
     */
    private void syncCollapseState() {
        for (Node node : hideWhenCollapsed) {
            node.setVisible(!collapsed);
            node.setManaged(!collapsed);
        }
        for (HBox content : itemContents) {
            content.setSpacing(collapsed ? 0 : CONTENT_SPACING);
            content.setAlignment(collapsed ? Pos.CENTER : Pos.CENTER_LEFT);
        }
        for (Button item : itemButtons) {
            String label = item.getUserData() == null ? "" : String.valueOf(item.getUserData());
            if (collapsed) {
                if (!tooltips.containsKey(item)) {
                    Tooltip tip = new Tooltip(label);
                    tooltips.put(item, tip);
                    Tooltip.install(item, tip);
                }
            } else {
                Tooltip tip = tooltips.remove(item);
                if (tip != null) {
                    Tooltip.uninstall(item, tip);
                }
            }
        }
        if (collapseLabel != null) {
            collapseLabel.setText(collapsed ? "Expand" : "Collapse");
            renderIconInto(collapseIconSlot, collapsed ? FontAwesomeSolid.ANGLE_DOUBLE_RIGHT : FontAwesomeSolid.ANGLE_DOUBLE_LEFT);
            Icons.tooltip(collapseButton, collapsed ? "Expand navigation" : "Collapse navigation");
        }
    }

    // ------------------------------------------------------------------
    // Active item
    // ------------------------------------------------------------------

    /** Marks the item with this label active; the previous one is cleared. */
    public void setActive(String label) {
        activeLabel = label;
        for (Button item : itemButtons) {
            boolean isActive = label != null && label.equals(item.getUserData());
            if (isActive) {
                if (!item.getStyleClass().contains("rail-item-active")) {
                    item.getStyleClass().add("rail-item-active");
                }
            } else {
                item.getStyleClass().remove("rail-item-active");
            }
            if (item.getGraphic() instanceof HBox content
                    && !content.getChildren().isEmpty()
                    && content.getChildren().get(0) instanceof Region bar) {
                bar.getStyleClass().removeAll("rail-item-bar", "rail-item-spacer");
                bar.getStyleClass().add(isActive ? "rail-item-bar" : "rail-item-spacer");
            }
        }
    }

    /**
     * Highlights whatever rail item corresponds to a screen title, so screens
     * opened from elsewhere leave the rail pointing at the right place rather
     * than at nothing.
     */
    public void setActiveByTitle(String title) {
        if (title == null) {
            return;
        }
        for (Button item : itemButtons) {
            if (title.equals(item.getUserData())) {
                setActive(title);
                return;
            }
        }
    }

    public Region node() {
        return rail;
    }

    public String getActiveLabel() {
        return activeLabel;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    // ------------------------------------------------------------------
    // The role trees
    // ------------------------------------------------------------------

    /**
     * The navigation each role is given. The three trees differ in structure, not
     * merely in a flag, and a role never sees an item it cannot open.
     */
    private static List<Entry> entriesFor(User user) {
        List<Entry> entries = new ArrayList<>();
        Role role = user.getRole() == null ? Role.TEACHER : user.getRole();
        entries.add(new Entry("", "Dashboard", FontAwesomeSolid.HOME, MainShell::showHome, null));

        switch (role) {
            case ADMINISTRATOR -> {
                entries.add(new Entry("Students", "Admit New Student", FontAwesomeSolid.USER_GRADUATE,
                        () -> AdmissionFormScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Students", "Students List", FontAwesomeSolid.USERS,
                        () -> StudentsListScreen.show(MainShell.getStage(), user), studentCount()));
                entries.add(new Entry("Students", "Search Students", FontAwesomeSolid.SEARCH,
                        () -> AdvancedSearchScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Students", "Requirements Checklist", FontAwesomeSolid.CLIPBOARD_CHECK,
                        () -> RequirementsScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Students", "Uniform Checklist", FontAwesomeSolid.TSHIRT,
                        () -> UniformScreen.show(MainShell.getStage(), user), null));

                entries.add(new Entry("Finance", "Record Student Payment", FontAwesomeSolid.MONEY_BILL_WAVE,
                        () -> PaymentScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Finance", "Treasury Dashboard", FontAwesomeSolid.CHART_LINE,
                        () -> TreasuryScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Finance", "Set Term Fees", FontAwesomeSolid.TAGS,
                        () -> FeeStructureScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Finance", "Manage / Pay Workers", FontAwesomeSolid.HARD_HAT,
                        () -> WorkerScreen.show(MainShell.getStage(), user), null));

                entries.add(new Entry("Academics", "Report Cards", FontAwesomeSolid.FILE_ALT,
                        () -> ReportCardScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Academics", "Term Management", FontAwesomeSolid.CALENDAR_ALT,
                        () -> TermManagementScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Academics", "School Events", FontAwesomeSolid.BULLHORN,
                        () -> ManageEventsScreen.show(MainShell.getStage(), user), null));

                entries.add(new Entry("Administration", "User Accounts", FontAwesomeSolid.USER_COG,
                        () -> UserManagementScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Administration", "Requirement Lists", FontAwesomeSolid.LIST_ALT,
                        () -> ManageRequirementTemplatesScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Administration", "Audit Log", FontAwesomeSolid.HISTORY,
                        () -> AuditLogScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Administration", "Correct Record Dates", FontAwesomeSolid.CLOCK,
                        () -> DateCorrectionScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Administration", "Check for Updates", FontAwesomeSolid.CLOUD_DOWNLOAD_ALT,
                        () -> UpdateScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Administration", "Backup Database", FontAwesomeSolid.DATABASE,
                        MainShell::backupDatabase, null));
            }
            case BURSAR -> {
                entries.add(new Entry("Students", "Admit New Student", FontAwesomeSolid.USER_GRADUATE,
                        () -> AdmissionFormScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Students", "Students List", FontAwesomeSolid.USERS,
                        () -> StudentsListScreen.show(MainShell.getStage(), user), studentCount()));
                entries.add(new Entry("Students", "Search Students", FontAwesomeSolid.SEARCH,
                        () -> AdvancedSearchScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Students", "Requirements Checklist", FontAwesomeSolid.CLIPBOARD_CHECK,
                        () -> RequirementsScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Students", "Uniform Checklist", FontAwesomeSolid.TSHIRT,
                        () -> UniformScreen.show(MainShell.getStage(), user), null));

                entries.add(new Entry("Finance", "Record Student Payment", FontAwesomeSolid.MONEY_BILL_WAVE,
                        () -> PaymentScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Finance", "Treasury Dashboard", FontAwesomeSolid.CHART_LINE,
                        () -> TreasuryScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Finance", "Set Term Fees", FontAwesomeSolid.TAGS,
                        () -> FeeStructureScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Finance", "Manage / Pay Workers", FontAwesomeSolid.HARD_HAT,
                        () -> WorkerScreen.show(MainShell.getStage(), user), null));

                entries.add(new Entry("Accounts", "Create Teacher Account", FontAwesomeSolid.USER_TIE,
                        () -> TeacherAccountScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Accounts", "Report Cards", FontAwesomeSolid.FILE_ALT,
                        () -> ReportCardScreen.show(MainShell.getStage(), user), null));
            }
            case TEACHER -> {
                entries.add(new Entry("Classroom", "Enter Results", FontAwesomeSolid.PEN,
                        () -> ResultsEntryScreen.show(MainShell.getStage(), user), null));
                entries.add(new Entry("Classroom", "Attendance / Return Confirmation", FontAwesomeSolid.CLIPBOARD_LIST,
                        () -> AttendanceScreen.show(MainShell.getStage(), user), null));
            }
        }
        return entries;
    }

    /** Student count for the rail's count pill; null when the database is unreadable. */
    private static Integer studentCount() {
        try {
            return new com.school.sms.dao.StudentDAO().findAll().size();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
