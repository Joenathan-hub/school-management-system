package com.school.sms.ui.design;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * The data table card.
 *
 * <p>Rows are laid out rather than handed to a {@code TableView}, for two
 * reasons: the design's cell metrics (8/14 padding inside a 1px-bordered card
 * that has no padding of its own) are easier to hold exactly this way, and rows
 * have to be arrow-key navigable and open on Enter, which the design asks for and
 * which a {@code TableView} does not do out of the box.</p>
 *
 * <p>Columns are sized by weight, never by fixed pixel widths, so the table stays
 * readable when the window is resized; money and counts are right-aligned and set
 * in the tabular family, which is what makes a column of figures line up on the
 * decimal point.</p>
 *
 * <p>Loading and empty states are shaped like the content they replace: a
 * skeleton of rows while data loads — never a spinner as the primary affordance
 * for tabular data — and, when there is nothing to show, a sentence naming what is
 * missing plus one action that resolves it.</p>
 */
public final class DataTable {

    /** What a cell holds, and therefore how it is aligned and weighted. */
    public enum Kind {
        TEXT, NAME, NUMERIC, MONEY, MUTED, BADGE
    }

    /** One cell: its text plus how it should be presented. */
    public record Cell(String text, Kind kind, Components.Tone tone) {

        public static Cell text(String text) {
            return new Cell(text, Kind.TEXT, null);
        }

        public static Cell name(String text) {
            return new Cell(text, Kind.NAME, null);
        }

        public static Cell muted(String text) {
            return new Cell(text, Kind.MUTED, null);
        }

        public static Cell number(String text) {
            return new Cell(text, Kind.NUMERIC, null);
        }

        public static Cell money(double amount) {
            return new Cell(Components.money(amount), Kind.MONEY, null);
        }

        public static Cell badge(String text, Components.Tone tone) {
            return new Cell(text, Kind.BADGE, tone == null ? Components.Tone.NEUTRAL : tone);
        }
    }

    /** One row: its cells plus what Enter (or a click) does. */
    public record Row(List<Cell> cells, Runnable open) {

        public static Row of(Runnable open, Cell... cells) {
            return new Row(List.of(cells), open);
        }
    }

    /** A column definition: title and relative width. */
    public record Column(String title, double weight, boolean numeric) {

        public static Column of(String title, double weight) {
            return new Column(title, weight, false);
        }

        public static Column numeric(String title, double weight) {
            return new Column(title, weight, true);
        }
    }

    private final String title;
    private final String subtitle;
    private final String viewAllText;
    private final Runnable viewAllAction;
    private final List<Column> columns;

    private final VBox card = new VBox();
    private final GridPane headerRow = new GridPane();
    private final VBox body = new VBox();
    private final HBox footer = new HBox();
    private final Label footerText = Components.label("", "table-footer-text");
    private final HBox pagination = new HBox();

    private List<Row> rows = new ArrayList<>();
    private int page;
    private int pageSize = 6;
    private IntConsumer onPageChange;
    private Node emptyState;
    private boolean loading;

    public DataTable(String title, String subtitle, String viewAllText, Runnable viewAllAction, Column... columns) {
        this.title = title;
        this.subtitle = subtitle;
        this.viewAllText = viewAllText;
        this.viewAllAction = viewAllAction;
        this.columns = List.of(columns);
        build();
    }

    private void build() {
        card.getStyleClass().addAll("table-card", "card-flush");

        Node viewAll = viewAllAction == null ? null
                : Components.linkButton(viewAllText, org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.CHEVRON_RIGHT, viewAllAction);
        card.getChildren().add(Components.cardHeader(title, subtitle, viewAll));

        headerRow.getStyleClass().add("column-header-background");
        applyColumns(headerRow);
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            Label header = Components.label(column.title(), "table-column-title");
            HBox cell = new HBox(header);
            cell.getStyleClass().add("column-header");
            if (column.numeric()) {
                cell.getStyleClass().add("column-numeric");
                cell.setAlignment(Pos.CENTER_RIGHT);
                HBox.setHgrow(header, Priority.ALWAYS);
                header.setMaxWidth(Double.MAX_VALUE);
                header.setAlignment(Pos.CENTER_RIGHT);
            }
            headerRow.add(cell, i, 0);
        }
        card.getChildren().add(headerRow);
        card.getChildren().add(body);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        footer.getStyleClass().add("table-footer");
        footer.getChildren().addAll(footerText, spacer, pagination);
        card.getChildren().add(footer);
    }

    private void applyColumns(GridPane grid) {
        grid.getColumnConstraints().clear();
        double total = columns.stream().mapToDouble(Column::weight).sum();
        for (Column column : columns) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(column.weight() / total * 100.0);
            constraints.setHgrow(Priority.ALWAYS);
            constraints.setFillWidth(true);
            grid.getColumnConstraints().add(constraints);
        }
    }

    // ------------------------------------------------------------------
    // Content
    // ------------------------------------------------------------------

    public void setRows(List<Row> newRows) {
        this.rows = newRows == null ? new ArrayList<>() : new ArrayList<>(newRows);
        this.page = 0;
        render();
    }

    public void setPageSize(int size) {
        this.pageSize = Math.max(1, size);
    }

    /** Called with the new page index when the user picks a page. */
    public void setOnPageChange(IntConsumer listener) {
        this.onPageChange = listener;
    }

    /** Replaces the table body with a skeleton shaped like the rows it will hold. */
    public void setLoading(boolean loading) {
        this.loading = loading;
        if (loading) {
            body.getChildren().setAll(skeletonRows());
            Components.startSkeletonPulse(body);
            footerText.setText("Loading\u2026");
            pagination.getChildren().clear();
        } else {
            render();
        }
    }

    /** Replaces the table body with an empty state until data arrives. */
    public void setEmptyState(Node emptyState) {
        this.emptyState = emptyState;
        if (!loading) {
            render();
        }
    }

    private Node skeletonRows() {
        VBox skeleton = new VBox();
        for (int r = 0; r < Math.min(pageSize, 6); r++) {
            GridPane row = new GridPane();
            row.getStyleClass().add("skeleton-table-row");
            applyColumns(row);
            for (int c = 0; c < columns.size(); c++) {
                Region block = new Region();
                block.getStyleClass().add("skeleton");
                block.setMinHeight(10);
                block.setPrefHeight(10);
                block.setPrefWidth(c == 0 ? 150 : (c == columns.size() - 1 ? 60 : 90));
                block.setMaxWidth(c == columns.size() - 1 ? 60 : Region.USE_PREF_SIZE);
                HBox wrap = new HBox(block);
                wrap.setAlignment(columns.get(c).numeric() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                row.add(wrap, c, 0);
            }
            skeleton.getChildren().add(row);
        }
        return skeleton;
    }

    private void render() {
        body.getChildren().clear();
        if (rows.isEmpty()) {
            if (emptyState != null) {
                body.getChildren().add(emptyState);
            } else {
                body.getChildren().add(Components.emptyState(
                        org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.INBOX,
                        "Nothing to show yet",
                        "There is no data for this table yet.",
                        null, null));
            }
            footerText.setText("No rows");
            pagination.getChildren().clear();
            return;
        }

        int pageCount = pageCount();
        int from = page * pageSize;
        int to = Math.min(rows.size(), from + pageSize);
        List<Row> visible = rows.subList(from, to);
        for (int i = 0; i < visible.size(); i++) {
            body.getChildren().add(rowNode(visible.get(i), i, visible.size()));
        }

        footerText.setText(String.format("Showing %d\u2013%d of %d", from + 1, to, rows.size()));
        buildPagination(pageCount);
    }

    private Node rowNode(Row row, int indexInPage, int rowsInPage) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("table-row-cell");
        grid.setFocusTraversable(true);
        applyColumns(grid);

        for (int c = 0; c < columns.size(); c++) {
            Cell cell = c < row.cells().size() ? row.cells().get(c) : Cell.text("");
            Node node = cellNode(cell);
            HBox wrap = new HBox(node);
            wrap.getStyleClass().add("table-cell");
            wrap.setAlignment(columns.get(c).numeric() || cell.kind() == Kind.MONEY || cell.kind() == Kind.NUMERIC
                    ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            HBox.setHgrow(node, Priority.ALWAYS);
            if (node instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
            }
            grid.add(wrap, c, 0);
        }

        grid.setOnMouseClicked(e -> {
            if (row.open() != null) {
                row.open().run();
            }
        });
        grid.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case DOWN -> {
                    focusRow(indexInPage + 1, rowsInPage);
                    e.consume();
                }
                case UP -> {
                    focusRow(indexInPage - 1, rowsInPage);
                    e.consume();
                }
                case HOME -> {
                    focusRow(0, rowsInPage);
                    e.consume();
                }
                case END -> {
                    focusRow(rowsInPage - 1, rowsInPage);
                    e.consume();
                }
                case ENTER -> {
                    if (row.open() != null) {
                        row.open().run();
                    }
                    e.consume();
                }
                default -> {
                }
            }
        });
        return grid;
    }

    private void focusRow(int index, int rowsInPage) {
        if (index < 0 || index >= rowsInPage) {
            return;
        }
        Node node = body.getChildren().get(index);
        if (node instanceof GridPane grid) {
            grid.requestFocus();
        }
    }

    private Node cellNode(Cell cell) {
        if (cell.kind() == Kind.BADGE) {
            return Components.badge(cell.text(), cell.tone());
        }
        Label label = switch (cell.kind()) {
            case NAME -> Components.label(cell.text(), "table-cell-text", "table-cell-name");
            case NUMERIC -> Components.label(cell.text(), "table-cell-numeric");
            case MONEY -> Components.label(cell.text(), "table-cell-numeric", "money-value");
            case MUTED -> Components.label(cell.text(), "table-cell-text", "column-muted");
            default -> Components.label(cell.text(), "table-cell-text");
        };
        return label;
    }

    // ------------------------------------------------------------------
    // Pagination
    // ------------------------------------------------------------------

    private int pageCount() {
        return Math.max(1, (int) Math.ceil(rows.size() / (double) pageSize));
    }

    private void buildPagination(int pageCount) {
        pagination.getStyleClass().add("pagination");
        pagination.getChildren().clear();
        pagination.getChildren().add(pageButton("\u2039", page > 0, page - 1));
        for (int i = 0; i < pageCount; i++) {
            pagination.getChildren().add(pageButton(String.valueOf(i + 1), true, i));
        }
        pagination.getChildren().add(pageButton("\u203a", page < pageCount - 1, page + 1));
    }

    private Button pageButton(String text, boolean enabled, int targetPage) {
        Button button = new Button(text);
        button.getStyleClass().add("page-btn");
        button.setDisable(!enabled);
        if (targetPage == page) {
            button.getStyleClass().add("page-btn-active");
            button.setDisable(false);
        }
        button.setOnAction(e -> {
            page = Math.max(0, Math.min(pageCount() - 1, targetPage));
            render();
            if (onPageChange != null) {
                onPageChange.accept(page);
            }
        });
        return button;
    }

    /** The assembled card. */
    public Node node() {
        return card;
    }
}
