package com.school.sms.ui.design;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The application's component library.
 *
 * <p>Every control in the interface is assembled here rather than in the screens,
 * so the design is applied once. Nothing in this class holds a colour, a size or a
 * font: all of that lives in structure.css / components.css, and the names used
 * below are the class names those sheets define.</p>
 *
 * <h2>Tone, not colour</h2>
 * Status is expressed with {@link Tone}, and a tone always travels with its word —
 * a badge reads "Overdue" as well as being toned, a banner says what happened in
 * text, and an icon is supplied for the eye. Nothing in this application ever
 * relies on a colour alone to carry meaning, which is also why the tone names are
 * semantic ("danger") rather than chromatic ("red").</p>
 */
public final class Components {

    /** Semantic status. Never used on its own to convey meaning. */
    public enum Tone {
        SUCCESS("badge-success", "banner-success", "toast-success"),
        WARNING("badge-warning", "banner-warning", "toast-warning"),
        DANGER("badge-danger", "banner-danger", "toast-danger"),
        INFO("badge-info", "banner-info", "toast-info"),
        NEUTRAL("badge-neutral", "banner-neutral", "toast-neutral");

        private final String badgeClass;
        private final String bannerClass;
        private final String toastClass;

        Tone(String badgeClass, String bannerClass, String toastClass) {
            this.badgeClass = badgeClass;
            this.bannerClass = bannerClass;
            this.toastClass = toastClass;
        }

        public String badgeClass() {
            return badgeClass;
        }

        public String bannerClass() {
            return bannerClass;
        }

        public String toastClass() {
            return toastClass;
        }

        /** The icon that accompanies this tone, so shape supports colour. */
        public Ikon icon() {
            return switch (this) {
                case SUCCESS -> org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.CHECK_CIRCLE;
                case WARNING -> org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.EXCLAMATION_TRIANGLE;
                case DANGER -> org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.EXCLAMATION_CIRCLE;
                case INFO -> org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.INFO_CIRCLE;
                case NEUTRAL -> org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.INFO_CIRCLE;
            };
        }
    }

    private Components() {
    }

    // ==================================================================
    // Text
    // ==================================================================

    public static Label label(String text, String... styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClasses);
        return label;
    }

    public static Label pageTitle(String text) {
        return label(text, "page-title");
    }

    public static Label pageSubtitle(String text) {
        Label label = label(text, "page-subtitle");
        label.setWrapText(true);
        return label;
    }

    public static Label cardTitle(String text) {
        return label(text, "card-title");
    }

    public static Label cardSubtitle(String text) {
        return label(text, "card-subtitle");
    }

    public static Label bodyText(String text) {
        return label(text, "body-text");
    }

    public static Label bodyTextStrong(String text) {
        return label(text, "body-text-strong");
    }

    public static Label fieldLabel(String text) {
        return label(text, "field-label");
    }

    public static Label sectionLabel(String text) {
        return label(text, "section-label");
    }

    /** A number that must line up with the numbers above and below it. */
    public static Label moneyValue(String text) {
        return label(text, "money-value");
    }

    // ==================================================================
    // Surfaces
    // ==================================================================

    /** A card: one pixel border, 9px radius, 14px padding, no shadow. */
    public static VBox card(Node... children) {
        VBox card = new VBox();
        card.getStyleClass().add("card");
        card.getChildren().addAll(children);
        return card;
    }

    /** A card with 12px between its children instead of the default. */
    public static VBox cardSpaced(Node... children) {
        VBox card = card(children);
        card.getStyleClass().add("card-gap-12");
        return card;
    }

    /** A card whose children run edge to edge (used by the data table). */
    public static VBox flushCard(Node... children) {
        VBox card = new VBox();
        card.getStyleClass().add("card-flush");
        card.getChildren().addAll(children);
        return card;
    }

    /**
     * The card header: title and subtitle on the left, actions on the right, inset
     * 13px vertically and 14px horizontally.
     */
    public static HBox cardHeader(String title, String subtitle, Node... actions) {
        VBox titles = new VBox();
        titles.getStyleClass().add("card-header-titles");
        titles.getChildren().add(cardTitle(title));
        if (subtitle != null && !subtitle.isBlank()) {
            titles.getChildren().add(cardSubtitle(subtitle));
        }
        HBox header = new HBox(titles);
        header.getStyleClass().add("card-header-box");
        if (actions.length > 0) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            header.getChildren().add(spacer);
            header.getChildren().addAll(actions);
        }
        return header;
    }

    /**
     * The page header: title and subtitle on the left, a secondary and a primary
     * action on the right.
     */
    public static HBox pageHeader(String title, String subtitle, Node... actions) {
        VBox titles = new VBox(2);
        titles.getChildren().add(pageTitle(title));
        if (subtitle != null && !subtitle.isBlank()) {
            titles.getChildren().add(pageSubtitle(subtitle));
        }
        HBox header = new HBox(titles);
        header.getStyleClass().add("page-header");
        if (actions.length > 0) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label filler = bodyText("");
            filler.setMinWidth(16);
            header.getChildren().addAll(spacer);
            HBox actionsBox = new HBox(8);
            actionsBox.getStyleClass().add("page-header-actions");
            actionsBox.setAlignment(Pos.CENTER_RIGHT);
            actionsBox.getChildren().addAll(actions);
            header.getChildren().add(actionsBox);
        }
        return header;
    }

    // ==================================================================
    // Buttons
    // ==================================================================

    public static Button primaryButton(String text, Ikon icon, Runnable action) {
        return button(text, icon, action, "btn", "btn-primary");
    }

    public static Button secondaryButton(String text, Ikon icon, Runnable action) {
        return button(text, icon, action, "btn", "btn-secondary");
    }

    public static Button ghostButton(String text, Ikon icon, Runnable action) {
        return button(text, icon, action, "btn", "btn-ghost");
    }

    public static Button dangerButton(String text, Ikon icon, Runnable action) {
        return button(text, icon, action, "btn", "btn-danger");
    }

    /** A text-only button, used by "view all" affordances. */
    public static Button linkButton(String text, Ikon icon, Runnable action) {
        return button(text, icon, action, "btn", "btn-link");
    }

    private static Button button(String text, Ikon icon, Runnable action, String... styleClasses) {
        Button button = new Button(text);
        button.getStyleClass().addAll(styleClasses);
        if (icon != null) {
            button.setGraphic(Icons.boxed(icon, Icons.INLINE));
        }
        if (action != null) {
            button.setOnAction(e -> action.run());
        }
        return button;
    }

    /**
     * An icon-only button. An icon-only control always gets a tooltip: there is no
     * visible label to explain it, and the 32&#215;32 target is comfortably above
     * the 28px minimum hit target.
     */
    public static Button iconButton(Ikon icon, String tooltip, Runnable action) {
        Button button = new Button();
        button.getStyleClass().addAll("icon-button");
        button.setGraphic(Icons.boxed(icon, Icons.INLINE));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        Icons.tooltip(button, tooltip);
        if (action != null) {
            button.setOnAction(e -> action.run());
        }
        return button;
    }

    /** The larger icon-only button, used for a page's leading action. */
    public static Button iconButtonLarge(Ikon icon, String tooltip, Runnable action) {
        Button button = new Button();
        button.getStyleClass().addAll("icon-button", "icon-button-lg");
        button.setGraphic(Icons.boxed(icon, Icons.TOPBAR));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        Icons.tooltip(button, tooltip);
        if (action != null) {
            button.setOnAction(e -> action.run());
        }
        return button;
    }

    // ==================================================================
    // Badges and pills
    // ==================================================================

    /**
     * A status badge. The text is mandatory: the design never lets a colour alone
     * say what a status is, so this factory has no overload that omits it.
     */
    public static Label badge(String text, Tone tone) {
        Label badge = label(text, "badge", tone == null ? Tone.NEUTRAL.badgeClass() : tone.badgeClass());
        if (tone != null) {
            badge.setGraphic(Icons.boxed(tone.icon(), Icons.PILL));
        }
        return badge;
    }

    /** A count in a pill: neutral, right-aligned, tabular. */
    public static Label countPill(int count) {
        return label(formatCount(count), "badge", "badge-neutral");
    }

    // ==================================================================
    // Banners and toasts
    // ==================================================================

    /**
     * An inline banner. Used where a message must stay put (a form that failed
     * validation), as opposed to a toast, which is transient.
     */
    public static HBox banner(String text, Tone tone, String actionText, Runnable action) {
        Tone effectiveTone = tone == null ? Tone.NEUTRAL : tone;
        HBox banner = new HBox(9);
        banner.getStyleClass().addAll("banner", effectiveTone.bannerClass());
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.getChildren().add(Icons.boxed(effectiveTone.icon(), Icons.INLINE, "banner-icon"));
        Label message = label(text, "banner-text");
        message.setWrapText(true);
        HBox.setHgrow(message, Priority.ALWAYS);
        banner.getChildren().add(message);
        if (actionText != null && action != null) {
            banner.getChildren().add(linkButton(actionText, null, action));
        }
        return banner;
    }

    /**
     * A transient message. Toasts are stacked in the window's toast layer (see
     * {@code MainShell#toastLayer()}) and dismiss themselves; they never carry an
     * action that the user cannot also reach somewhere permanent.
     */
    public static HBox toast(String text, Tone tone, Runnable onDismiss) {
        Tone effectiveTone = tone == null ? Tone.NEUTRAL : tone;
        HBox toast = new HBox(9);
        toast.getStyleClass().addAll("toast", effectiveTone.toastClass());
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.getChildren().add(Icons.boxed(effectiveTone.icon(), Icons.INLINE, "toast-icon"));
        Label message = label(text, "toast-text");
        message.setWrapText(true);
        HBox.setHgrow(message, Priority.ALWAYS);
        toast.getChildren().add(message);
        Button dismiss = iconButton(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.TIMES, "Dismiss", onDismiss);
        dismiss.getStyleClass().add("btn-ghost");
        toast.getChildren().add(dismiss);
        return toast;
    }

    // ==================================================================
    // Avatars
    // ==================================================================

    /**
     * The gradient avatar chip. Initials rather than a photograph: there is no
     * avatar storage in this application, and initials at 11px/800 are legible at
     * 30px where a downscaled photograph would not be.
     */
    public static StackPane avatar(String fullName) {
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("avatar");
        Label initials = label(initialsOf(fullName), "avatar-text");
        avatar.getChildren().add(initials);
        avatar.setMinSize(30, 30);
        avatar.setPrefSize(30, 30);
        avatar.setMaxSize(30, 30);
        return avatar;
    }

    static String initialsOf(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty() && initials.length() < 2) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return initials.length() == 0 ? "?" : initials.toString();
    }

    // ==================================================================
    // Inputs
    // ==================================================================

    public static TextField textField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("text-field");
        return field;
    }

    public static PasswordField passwordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.getStyleClass().add("password-field");
        return field;
    }

    public static TextArea textArea(String prompt, int rows) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefRowCount(rows);
        area.setWrapText(true);
        area.getStyleClass().add("text-area");
        return area;
    }

    /**
     * A labelled field with a hint and an error slot. Validation errors are
     * announced as text under the control, never by turning the border red on its
     * own.
     */
    public static VBox field(String labelText, Node input, String hint) {
        VBox box = new VBox(6);
        box.getStyleClass().add("field");
        if (labelText != null) {
            box.getChildren().add(fieldLabel(labelText));
        }
        box.getChildren().add(input);
        Label hintLabel = label(hint == null ? "" : hint, "field-hint");
        hintLabel.setWrapText(true);
        hintLabel.setManaged(hint != null && !hint.isBlank());
        hintLabel.setVisible(hint != null && !hint.isBlank());
        box.getChildren().add(hintLabel);
        Label errorLabel = label("", "field-error-text");
        errorLabel.setWrapText(true);
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
        box.getChildren().add(errorLabel);
        return box;
    }

    /**
     * Marks a field invalid and explains why. Adding the error class and showing
     * the message always happen together.
     */
    public static void showFieldError(VBox fieldBox, String message) {
        Node input = fieldBox.getChildren().size() > 1 ? fieldBox.getChildren().get(1) : null;
        if (input != null) {
            if (!input.getStyleClass().contains("field-error")) {
                input.getStyleClass().add("field-error");
            }
        }
        for (Node child : fieldBox.getChildren()) {
            if (child.getStyleClass().contains("field-error-text")) {
                ((Label) child).setText(message);
                child.setManaged(true);
                child.setVisible(true);
            }
        }
    }

    public static void clearFieldError(VBox fieldBox) {
        for (Node child : fieldBox.getChildren()) {
            if (child.getStyleClass().contains("field-error")) {
                child.getStyleClass().remove("field-error");
            }
            if (child.getStyleClass().contains("field-error-text")) {
                ((Label) child).setText("");
                child.setManaged(false);
                child.setVisible(false);
            }
        }
    }

    /** The search field with its leading glyph and trailing keyboard hint. */
    public static HBox searchField(String prompt, Runnable onActivate) {
        HBox field = new HBox();
        field.getStyleClass().add("search-field");
        field.setAlignment(Pos.CENTER_LEFT);
        field.getChildren().add(Icons.boxed(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.SEARCH,
                Icons.TOPBAR, "search-icon"));
        TextField input = new TextField();
        input.setPromptText(prompt);
        input.getStyleClass().add("search-placeholder-size");
        HBox.setHgrow(input, Priority.ALWAYS);
        field.getChildren().add(input);
        if (onActivate != null) {
            input.setOnMouseClicked(e -> onActivate.run());
        }
        field.getChildren().add(kbdHint("Ctrl K"));
        return field;
    }

    /** A keyboard hint chip. Shows the platform's own modifier, not a hardcoded one. */
    public static HBox kbdHint(String text) {
        HBox hint = new HBox();
        hint.getStyleClass().add("kbd-hint");
        hint.setAlignment(Pos.CENTER);
        hint.getChildren().add(label(text, "kbd-hint-text"));
        return hint;
    }

    // ==================================================================
    // Segmented control, toggle switch
    // ==================================================================

    /**
     * A segmented control. Selection is shown by the selected item's own
     * background <em>and</em> its weight, and the whole control is keyboard
     * reachable: each segment is a focusable toggle in a group, so arrow keys and
     * Space work without a mouse.
     */
    public static HBox segmented(List<String> options, int selectedIndex, Consumer<Integer> onSelect) {
        HBox control = new HBox();
        control.getStyleClass().add("segmented");
        List<ToggleButton> segments = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            int index = i;
            ToggleButton segment = new ToggleButton(options.get(i));
            segment.getStyleClass().add("segmented-item");
            segment.setSelected(i == selectedIndex);
            if (i == selectedIndex) {
                segment.getStyleClass().add("segmented-item-selected");
            }
            segment.setOnAction(e -> {
                for (ToggleButton other : segments) {
                    other.setSelected(false);
                    other.getStyleClass().remove("segmented-item-selected");
                }
                segment.setSelected(true);
                if (!segment.getStyleClass().contains("segmented-item-selected")) {
                    segment.getStyleClass().add("segmented-item-selected");
                }
                if (onSelect != null) {
                    onSelect.accept(index);
                }
            });
            segments.add(segment);
            control.getChildren().add(segment);
        }
        return control;
    }

    /**
     * A toggle switch. The state is announced in text by the caller (a label that
     * changes), so the switch is never the only evidence of what is on.
     */
    public static StackPane toggleSwitch(boolean on, Consumer<Boolean> onChange) {
        StackPane control = new StackPane();
        control.getStyleClass().add("toggle-switch");
        Region thumb = new Region();
        thumb.getStyleClass().add("toggle-thumb");
        control.getChildren().add(thumb);
        control.setMinSize(38, 22);
        control.setPrefSize(38, 22);
        control.setMaxSize(38, 22);
        control.setFocusTraversable(true);
        control.getStyleClass().add(on ? "toggle-on" : "toggle-off");
        setToggleState(control, thumb, on);
        control.setOnMouseClicked(e -> {
            boolean next = !control.getStyleClass().contains("toggle-on");
            setToggleState(control, thumb, next);
            if (onChange != null) {
                onChange.accept(next);
            }
        });
        control.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case SPACE, ENTER -> {
                    boolean next = !control.getStyleClass().contains("toggle-on");
                    setToggleState(control, thumb, next);
                    if (onChange != null) {
                        onChange.accept(next);
                    }
                    e.consume();
                }
                default -> {
                }
            }
        });
        return control;
    }

    private static void setToggleState(StackPane control, Region thumb, boolean on) {
        control.getStyleClass().removeAll("toggle-on", "toggle-off");
        control.getStyleClass().add(on ? "toggle-on" : "toggle-off");
        thumb.setTranslateX(on ? 8 : -8);
    }

    // ==================================================================
    // Progress
    // ==================================================================

    /**
     * One labelled progress row: label on the left, the figure in a fixed-width
     * right-aligned column, and the track underneath. The percentage is always
     * printed, so the row reads correctly without colour.
     */
    public static VBox progressRow(String label, double fraction) {
        double clamped = Math.max(0, Math.min(1, fraction));
        HBox line = new HBox(6);
        Label name = label(label, "progress-label");
        HBox.setHgrow(name, Priority.ALWAYS);
        name.setMaxWidth(Double.MAX_VALUE);
        StackPane percentCell = new StackPane(label(formatPercent(clamped), "progress-value"));
        percentCell.getStyleClass().add("progress-value-cell");
        line.getChildren().addAll(name, percentCell);

        StackPane track = new StackPane();
        track.getStyleClass().add("progress-track");
        Region fill = new Region();
        fill.getStyleClass().add("progress-fill");
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        track.getChildren().add(fill);
        track.widthProperty().addListener((obs, old, width) ->
                fill.setPrefWidth(Math.max(0, width.doubleValue() * clamped)));
        fill.setPrefWidth(0);

        VBox row = new VBox(6);
        row.getStyleClass().add("progress-row");
        row.getChildren().addAll(line, track);
        return row;
    }

    // ==================================================================
    // Skeletons
    // ==================================================================

    /**
     * A block of skeleton lines, shaped like the text it stands in for. Used for
     * tabular and list content: an indeterminate spinner is never the primary
     * loading affordance here, because it says nothing about what is coming.
     */
    public static VBox skeletonLines(int count) {
        VBox box = new VBox(8);
        for (int i = 0; i < count; i++) {
            Region line = new Region();
            line.getStyleClass().addAll("skeleton", i == 0 ? "skeleton-line-title" : "skeleton-line");
            line.setMaxWidth(i == 0 ? 180 : 260);
            line.setPrefWidth(i == 0 ? 180 : 260);
            box.getChildren().add(line);
        }
        return box;
    }

    public static Region skeletonKpiValue() {
        Region block = new Region();
        block.getStyleClass().addAll("skeleton", "skeleton-kpi");
        return block;
    }

    /** Starts the skeleton pulse on a node and its descendants. */
    public static void startSkeletonPulse(Node skeletonRoot) {
        Motion.pulse(skeletonRoot);
        for (Node child : skeletonRoot.lookupAll(".skeleton")) {
            Motion.pulse(child);
        }
    }

    // ==================================================================
    // Empty states
    // ==================================================================

    /**
     * An empty state: what is missing, in words, plus exactly one action that
     * resolves it. The action is mandatory — an empty state with nothing to do
     * about it is a dead end.
     */
    public static VBox emptyState(Ikon icon, String title, String message, String actionText, Runnable action) {
        VBox box = new VBox();
        box.getStyleClass().add("empty-state");
        StackPane iconChip = new StackPane();
        iconChip.getStyleClass().add("empty-icon");
        iconChip.getChildren().add(Icons.boxed(icon, Icons.LARGE));
        box.getChildren().addAll(iconChip, label(title, "empty-title"));
        Label text = label(message, "empty-text");
        text.setWrapText(true);
        text.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        box.getChildren().add(text);
        if (actionText != null && action != null) {
            box.getChildren().add(primaryButton(actionText, null, action));
        }
        return box;
    }

    // ==================================================================
    // Filter panel
    // ==================================================================

    /**
     * A collapsible filter panel. The header states how many filters are active,
     * so a collapsed panel never hides the fact that results are filtered.
     */
    public static VBox filterPanel(String title, Node body, int activeFilters) {
        VBox panel = new VBox();
        panel.getStyleClass().add("filter-panel");
        Label summary = label(activeFilters == 0 ? "No filters applied" : activeFilters + " filter(s) applied",
                "filter-panel-summary");
        HBox header = new HBox(8);
        header.getStyleClass().add("filter-panel-header");
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = label(title, "card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Node chevron = Icons.boxed(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.CHEVRON_DOWN, Icons.INLINE);
        header.getChildren().addAll(titleLabel, summary, spacer, chevron);
        VBox bodyBox = new VBox(body);
        bodyBox.getStyleClass().add("filter-panel-body");
        header.setOnMouseClicked(e -> {
            boolean showing = bodyBox.isVisible();
            bodyBox.setVisible(!showing);
            bodyBox.setManaged(!showing);
        });
        panel.getChildren().addAll(header, bodyBox);
        return panel;
    }

    // ==================================================================
    // Formatting
    // ==================================================================

    /** Formats money the way the whole application prints it: UGX, no decimals. */
    public static String money(double amount) {
        return String.format("UGX %,.0f", amount);
    }

    public static String moneyShort(double amount) {
        if (Math.abs(amount) >= 1_000_000) {
            return String.format("UGX %.1fM", amount / 1_000_000);
        }
        if (Math.abs(amount) >= 1_000) {
            return String.format("UGX %.0fK", amount / 1_000);
        }
        return String.format("UGX %,.0f", amount);
    }

    public static String percent(double fraction) {
        return formatPercent(fraction);
    }

    private static String formatPercent(double fraction) {
        return String.format("%.0f%%", fraction * 100);
    }

    public static String formatCount(int count) {
        return String.format("%,d", count);
    }
}
