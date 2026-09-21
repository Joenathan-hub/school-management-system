package com.school.sms.ui.design;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * The grouped bar chart.
 *
 * <p>Twelve columns, each a pair of bars: a pale companion bar (the comparison
 * series) beside the brand-coloured bar for the current series. No gridlines, no
 * axis lines, no borders, no 3D — the only reference is a single hairline marking
 * the target, and a legend that sits in the card header rather than floating over
 * the plot.</p>
 *
 * <p>The chart holds no colours of its own: bars are plain regions carrying
 * {@code chart-bar} / {@code chart-bar-alt}, and the two series colours come from
 * the token sheets. Only the heights are computed here, from the data.</p>
 *
 * <p>When there is nothing to plot the chart says so in words and offers the
 * action that would create data, rather than drawing an empty grid.</p>
 */
public final class PairedBarChart {

    /** One column: its month label and the two values (0..1 of the plot height). */
    public record Point(String label, double primary, double companion) {
    }

    private final String title;
    private final String subtitle;
    private final String primaryLabel;
    private final String companionLabel;
    private final double targetFraction;

    private final VBox card = new VBox();
    private final VBox footer = new VBox();

    private List<Point> points = new ArrayList<>();
    private Node emptyState;
    private String scaleCaption = "";

    /**
     * @param title           card heading
     * @param subtitle        muted line under the heading, usually the period covered
     * @param primaryLabel    legend text for the brand-coloured series
     * @param companionLabel  legend text for the pale companion series
     * @param targetFraction  where the reference line sits, 0..1 of the plot height (negative to omit)
     */
    public PairedBarChart(String title, String subtitle, String primaryLabel, String companionLabel, double targetFraction) {
        this.title = title;
        this.subtitle = subtitle;
        this.primaryLabel = primaryLabel;
        this.companionLabel = companionLabel;
        this.targetFraction = targetFraction;
        build();
    }

    private void build() {
        card.getStyleClass().add("chart-card");
        card.getChildren().add(Components.cardHeader(title, subtitle, legend()));
        card.getChildren().add(footer);
    }

    private Node legend() {
        HBox legend = new HBox();
        legend.getStyleClass().add("chart-legend");
        legend.setAlignment(Pos.CENTER_RIGHT);
        legend.getChildren().addAll(
                legendItem("chart-swatch-primary", primaryLabel),
                legendItem("chart-swatch-companion", companionLabel));
        return legend;
    }

    private Node legendItem(String swatchClass, String text) {
        Region swatch = new Region();
        swatch.getStyleClass().addAll("chart-swatch", swatchClass);
        HBox item = new HBox();
        item.getStyleClass().add("chart-legend-item");
        item.setAlignment(Pos.CENTER_LEFT);
        item.getChildren().addAll(swatch, Components.label(text, "chart-legend-label"));
        return item;
    }

    /**
     * Sets the plotted data.
     *
     * @param newPoints  one entry per column, left to right
     * @param caption    the muted line under the plot that states the scale, e.g.
     *                   "Tallest bar: UGX 4.2M in March"
     * @param emptyState shown instead of the plot when there is nothing to plot
     */
    public void setData(List<Point> newPoints, String caption, Node emptyState) {
        this.points = newPoints == null ? new ArrayList<>() : new ArrayList<>(newPoints);
        this.scaleCaption = caption == null ? "" : caption;
        this.emptyState = emptyState;
        render();
    }

    private void render() {
        footer.getChildren().clear();

        if (points.isEmpty()) {
            if (emptyState != null) {
                footer.getChildren().add(emptyState);
            } else {
                footer.getChildren().add(Components.emptyState(
                        org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.CHART_BAR,
                        "No data to plot yet",
                        "Once payments are recorded, this chart fills in month by month.",
                        null, null));
            }
            return;
        }

        // Bars are positioned as a fraction of the 120px plot, which is the
        // height the stylesheet gives .chart-plot.
        HBox columns = new HBox();
        columns.getStyleClass().add("chart-plot");
        columns.setAlignment(Pos.BOTTOM_CENTER);
        HBox monthRow = new HBox();
        monthRow.getStyleClass().add("chart-month-row");
        monthRow.setAlignment(Pos.CENTER);

        for (Point point : points) {
            VBox column = new VBox();
            column.getStyleClass().add("chart-column");
            column.setAlignment(Pos.BOTTOM_CENTER);
            column.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(column, Priority.ALWAYS);

            HBox pair = new HBox();
            pair.getStyleClass().add("chart-bar-pair");
            pair.setAlignment(Pos.BOTTOM_CENTER);
            pair.getChildren().addAll(bar(point.companion(), "chart-bar", "chart-bar-alt"), bar(point.primary(), "chart-bar"));
            column.getChildren().add(pair);

            StackPane monthCell = new StackPane(Components.label(point.label(), "chart-month-label"));
            monthCell.getStyleClass().add("chart-month-cell");
            monthCell.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(monthCell, Priority.ALWAYS);

            columns.getChildren().add(column);
            monthRow.getChildren().add(monthCell);
        }

        StackPane plot = new StackPane(columns);
        plot.setAlignment(Pos.BOTTOM_LEFT);
        if (targetFraction >= 0 && targetFraction <= 1) {
            Region target = new Region();
            target.getStyleClass().add("chart-target-line");
            target.setMaxWidth(Double.MAX_VALUE);
            // The plot is 120px tall (see .chart-plot); the line sits at the
            // target from the bottom, which is (1 - target) from the top.
            target.setTranslateY((1 - targetFraction) * 120.0);
            StackPane.setAlignment(target, Pos.TOP_LEFT);
            plot.getChildren().add(target);
        }

        footer.getChildren().addAll(plot, monthRow);
        if (!scaleCaption.isBlank()) {
            Label caption = Components.label(scaleCaption, "chart-legend-label");
            caption.getStyleClass().add("column-muted");
            footer.getChildren().add(caption);
        }
    }

    private Region bar(double fraction, String... styleClasses) {
        Region bar = new Region();
        bar.getStyleClass().addAll(styleClasses);
        double clamped = Math.max(0, Math.min(1, fraction));
        // A zero value still shows as a 2px plinth so the column is not empty space.
        double height = Math.max(2, clamped * 120.0);
        bar.setMinHeight(height);
        bar.setPrefHeight(height);
        bar.setMaxHeight(height);
        return bar;
    }

    public Node node() {
        return card;
    }
}
