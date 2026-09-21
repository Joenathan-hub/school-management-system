package com.school.sms.ui.design;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Vector icons, everywhere.
 *
 * <p>The application never ships a raster icon: every glyph is a
 * {@link FontIcon} from Ikonli's FontAwesome 5 pack, so it takes its size from
 * here, its colour from the stylesheets, and stays crisp on any display.</p>
 *
 * <p>Icons are wrapped in a fixed-size {@link Region} by {@link #boxed}: glyph
 * advance widths differ from glyph to glyph, and without a fixed slot a label
 * sitting next to an icon would shift horizontally as the navigation moved
 * between items. The box is what the layout sees; the glyph sits centred in
 * it.</p>
 *
 * <p>If the icon font cannot be loaded at all (a broken packaging step, say),
 * {@link #boxed} degrades to a neutral text glyph rather than throwing. The
 * application stays usable; the tooltip or label next to the icon still says what
 * the control does.</p>
 */
public final class Icons {

    /** Navigation rail glyphs. */
    public static final int NAV = 16;
    /** Top bar, cards and page headers. */
    public static final int TOPBAR = 14;
    /** Buttons, badges, inline row affordances. */
    public static final int INLINE = 13;
    /** Empty states and dialog headers. */
    public static final int LARGE = 20;
    /** Count pills and status badges. */
    public static final int PILL = 11;

    private static Boolean available;

    private Icons() {
    }

    /**
     * A bare glyph node. Prefer {@link #boxed} anywhere the icon shares a row with
     * text.
     */
    public static Node of(Ikon icon, int size, String... styleClasses) {
        Node node = tryGlyph(icon, size, styleClasses);
        if (node == null) {
            Label fallback = new Label("\u2022");
            fallback.getStyleClass().add("icon-fallback");
            return fallback;
        }
        return node;
    }

    /**
     * A glyph centred in a fixed {@code size} &#215; {@code size} slot. This is the
     * form used by navigation items, buttons and card headers.
     *
     * @param icon         the glyph
     * @param size         both the glyph size and the slot size, in pixels
     * @param styleClasses classes applied to the slot <em>and</em> to the glyph, so a
     *                     single context rule such as {@code .rail-item .rail-item-icon}
     *                     can colour either one
     */
    public static Region boxed(Ikon icon, int size, String... styleClasses) {
        StackPane slot = new StackPane();
        slot.getStyleClass().add("icon-slot");
        Node glyph = tryGlyph(icon, size, styleClasses);
        if (glyph == null) {
            Label fallback = new Label("\u2022");
            fallback.getStyleClass().add("icon-fallback");
            glyph = fallback;
        }
        slot.getChildren().add(glyph);
        slot.getStyleClass().addAll(styleClasses);
        slot.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        slot.setPrefSize(size, size);
        slot.setMaxSize(size, size);
        return slot;
    }

    /** True when the icon font is installed and glyphs are real vector icons. */
    public static boolean isAvailable() {
        if (available == null) {
            available = tryGlyph(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.HOME, NAV) != null;
        }
        return available;
    }

    private static Node tryGlyph(Ikon icon, int size, String... styleClasses) {
        try {
            FontIcon glyph = new FontIcon(icon);
            glyph.setIconSize(size);
            glyph.getStyleClass().addAll(styleClasses);
            return glyph;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Attaches a tooltip. Used for the collapsed rail, where an item's label is
     * hidden, and for every icon-only button — an icon-only control with no
     * tooltip would be unusable, and the hit target rule means at least one of the
     * two always applies.
     */
    public static void tooltip(Node node, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        Tooltip tip = new Tooltip(text);
        tip.setShowDelay(Duration.millis(350));
        tip.setHideDelay(Duration.millis(80));
        Tooltip.install(node, tip);
    }
}
