package com.school.sms.ui;

import com.school.sms.model.Role;
import com.school.sms.model.Student;
import com.school.sms.model.User;
import com.school.sms.service.DashboardKpiService;
import com.school.sms.ui.design.Components;
import com.school.sms.ui.design.DataTable;
import com.school.sms.ui.design.PairedBarChart;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The dashboard.
 *
 * <p>Reading order is deliberate: what happened today (four KPI figures), then how
 * the year is going (the chart beside the per-class progress rows), then the
 * individual students behind those figures (the table).</p>
 *
 * <p>Every number here comes from the database through
 * {@link DashboardKpiService}. Nothing is invented: where the application has no
 * historical snapshot to compare against — which is the case for all four KPI
 * cards — the card says what the figure is made of instead of showing a
 * period-over-period delta that would have to be fabricated.</p>
 */
public final class DashboardHomeScreen {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy");

    private DashboardHomeScreen() {
    }

    /** Builds the dashboard for the signed-in user. */
    public static Node build(User user) {
        DashboardKpiService kpi = new DashboardKpiService();
        VBox page = new VBox();
        page.getStyleClass().add("dashboard-page");

        page.getChildren().add(pageHeader(user));

        if (user.getRole() == Role.TEACHER) {
            page.getChildren().add(teacherNotice());
        }

        page.getChildren().add(kpiGrid(kpi));

        HBox middle = new HBox();
        middle.getStyleClass().add("panel-row");
        middle.setAlignment(Pos.TOP_LEFT);
        Node chart = chartCard(kpi);
        Node progress = progressCard(kpi);
        HBox.setHgrow(chart, Priority.ALWAYS);
        HBox.setHgrow(progress, Priority.ALWAYS);
        if (chart instanceof Region chartRegion) {
            chartRegion.setMaxWidth(Double.MAX_VALUE);
        }
        if (progress instanceof Region progressRegion) {
            progressRegion.setMaxWidth(Double.MAX_VALUE);
        }
        middle.getChildren().addAll(chart, progress);
        page.getChildren().add(middle);

        page.getChildren().add(studentsTable(user, kpi));
        return page;
    }

    // ------------------------------------------------------------------
    // Page header
    // ------------------------------------------------------------------

    private static Node pageHeader(User user) {
        String name = user.getFullName() == null ? "" : user.getFullName().split("\\s+")[0];
        String greeting = name.isBlank() ? "Dashboard" : "Good day, " + name;
        String subtitle = LocalDate.now().format(DATE_FMT);
        try {
            com.school.sms.model.Term active = new com.school.sms.dao.TermDAO().findActive();
            String term = active == null ? null : active.getName();
            if (term != null && !term.isBlank()) {
                subtitle = subtitle + " \u00b7 " + term;
            }
        } catch (Throwable ignored) {
            // The date alone is a fine subtitle.
        }

        Node export = Components.secondaryButton("Export", FontAwesomeSolid.FILE_EXPORT,
                () -> MainShell.toast("Exports are available from the Treasury screen.", Components.Tone.INFO));
        Node record = Components.primaryButton("Record payment", FontAwesomeSolid.PLUS,
                () -> PaymentScreen.show(MainShell.getStage(), user));

        List<Node> actions = new ArrayList<>();
        actions.add(export);
        actions.add(record);
        return Components.pageHeader(greeting, subtitle, actions.toArray(new Node[0]));
    }

    /** Teachers see their own workspace; the finance figures are not theirs to read. */
    private static Node teacherNotice() {
        return Components.banner(
                "You are signed in as a teacher: the figures below are school-wide and read-only for your role.",
                Components.Tone.INFO, null, null);
    }

    // ------------------------------------------------------------------
    // KPI cards
    // ------------------------------------------------------------------

    private static Node kpiGrid(DashboardKpiService kpi) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("kpi-grid");

        double collected;
        double expenditure;
        double outstanding;
        int notCleared;
        int collectedCount;
        int expenditureCount;
        try {
            collected = kpi.getCollectedTodayTotal();
            collectedCount = kpi.getCollectedTodayDetails().size();
            expenditure = kpi.getExpenditureTodayTotal();
            expenditureCount = kpi.getExpenditureTodayDetails().size();
            outstanding = kpi.getOutstandingTotal();
            notCleared = kpi.getStudentsNotClearedCount();
        } catch (Throwable failure) {
            // A database that cannot be read still gets a dashboard, with the
            // figures replaced by a banner explaining what went wrong.
            VBox fallback = new VBox(Components.banner(
                    "Figures could not be loaded: " + failure.getMessage(), Components.Tone.DANGER, null, null));
            fallback.getStyleClass().add("card");
            return fallback;
        }

        int classCount = 0;
        try {
            classCount = kpi.getOutstandingByClass().size();
        } catch (Throwable ignored) {
            // Sub-caption only.
        }

        grid.add(kpiCard("Collected today", Components.money(collected),
                collectedCount + (collectedCount == 1 ? " payment" : " payments") + " recorded",
                true), 0, 0);
        grid.add(kpiCard("Outstanding balance", Components.money(outstanding),
                classCount == 0 ? "No active term set" : "Across " + classCount + " class(es)", false), 1, 0);
        grid.add(kpiCard("Students not cleared", Components.formatCount(notCleared),
                "Fees or requirements outstanding", false), 2, 0);
        grid.add(kpiCard("Expenditure today", Components.money(expenditure),
                expenditureCount + (expenditureCount == 1 ? " entry" : " entries") + " recorded", false), 3, 0);

        for (int i = 0; i < 4; i++) {
            javafx.scene.layout.ColumnConstraints constraints = new javafx.scene.layout.ColumnConstraints();
            constraints.setPercentWidth(25);
            constraints.setHgrow(Priority.ALWAYS);
            constraints.setFillWidth(true);
            grid.getColumnConstraints().add(constraints);
        }
        return grid;
    }

    /**
     * A KPI card: label, figure, then a line saying what the figure is made of.
     * The figure is set in the tabular family so it lines up with the figures in
     * the other cards.
     */
    private static Node kpiCard(String label, String value, String subtext, boolean emphasis) {
        VBox card = new VBox();
        card.getStyleClass().add("kpi-card");
        if (emphasis) {
            card.getStyleClass().add("kpi-card-emphasis");
        }
        card.getChildren().add(Components.label(label.toUpperCase(Locale.ROOT), "kpi-label"));
        card.getChildren().add(Components.label(value, "kpi-value"));
        HBox note = new HBox(6);
        note.setAlignment(Pos.CENTER_LEFT);
        Label sub = Components.label(subtext, "kpi-delta", "kpi-delta-flat");
        note.getChildren().add(sub);
        card.getChildren().add(note);
        return card;
    }

    // ------------------------------------------------------------------
    // Chart
    // ------------------------------------------------------------------

    private static Node chartCard(DashboardKpiService kpi) {
        PairedBarChart chart = new PairedBarChart("Collections by month", "Fees received, this year",
                "This year", "Last year", 0.75);

        List<PairedBarChart.Point> points = monthlyCollections();
        double peak = points.stream().mapToDouble(p -> Math.max(p.primary(), p.companion())).max().orElse(0);
        String peakMonth = "";
        for (PairedBarChart.Point point : points) {
            if (Math.max(point.primary(), point.companion()) >= peak && peak > 0) {
                peakMonth = point.label();
            }
        }
        String caption = peak <= 0
                ? ""
                : "Tallest month: " + peakMonth + " \u00b7 bars are scaled to " + Components.moneyShort(peak);

        Node empty = Components.emptyState(FontAwesomeSolid.CHART_BAR, "No payments recorded yet",
                "Once fees are recorded this year, each month's collections appear here.",
                "Record a payment", () -> PaymentScreen.show(MainShell.getStage(), MainShell.getUser()));
        chart.setData(points, caption, empty);
        return chart.node();
    }

    /**
     * Twelve months of collections for this year and last, as fractions of the
     * tallest bar. Payment history is real: it comes from the payments table, not
     * from a projection.
     */
    private static List<PairedBarChart.Point> monthlyCollections() {
        double[] thisYear = new double[12];
        double[] lastYear = new double[12];
        int year = LocalDate.now().getYear();
        try {
            for (com.school.sms.model.Payment payment : new com.school.sms.dao.PaymentDAO().findAll()) {
                if (payment.getPaymentDate() == null) {
                    continue;
                }
                int paymentYear = payment.getPaymentDate().getYear();
                int monthIndex = payment.getPaymentDate().getMonthValue() - 1;
                if (paymentYear == year) {
                    thisYear[monthIndex] += payment.getAmount();
                } else if (paymentYear == year - 1) {
                    lastYear[monthIndex] += payment.getAmount();
                }
            }
        } catch (Throwable ignored) {
            // An unreadable payments table leaves the chart empty rather than broken.
        }

        double peak = 0;
        for (int i = 0; i < 12; i++) {
            peak = Math.max(peak, Math.max(thisYear[i], lastYear[i]));
        }

        List<PairedBarChart.Point> points = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            String label = Month.of(i + 1).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            double primary = peak <= 0 ? 0 : thisYear[i] / peak;
            double companion = peak <= 0 ? 0 : lastYear[i] / peak;
            points.add(new PairedBarChart.Point(label, primary, companion));
        }
        return points;
    }

    // ------------------------------------------------------------------
    // Progress rows
    // ------------------------------------------------------------------

    /**
     * Per-class collection progress: how much of each class's billed fees has been
     * received. Every row prints its percentage, so the bars are a summary of a
     * number that is also on screen.
     */
    private static Node progressCard(DashboardKpiService kpi) {
        VBox card = Components.cardSpaced();
        card.getChildren().add(Components.cardHeader("Collection progress", "Share of fees received, by class"));

        VBox rows = new VBox();
        rows.getStyleClass().add("progress-rows");

        Map<String, Double> outstandingByClass;
        try {
            outstandingByClass = kpi.getOutstandingByClass();
        } catch (Throwable failure) {
            outstandingByClass = Map.of();
        }

        if (outstandingByClass.isEmpty()) {
            rows.getChildren().add(Components.emptyState(FontAwesomeSolid.CHART_LINE,
                    "No class balances yet",
                    "Set term fees for the active term and each class's progress appears here.",
                    "Set term fees", () -> FeeStructureScreen.show(MainShell.getStage(), MainShell.getUser())));
        } else {
            double billed = 0;
            try {
                billed = kpi.getOutstandingTotal() + totalCollected();
            } catch (Throwable ignored) {
                billed = 0;
            }
            for (Map.Entry<String, Double> entry : outstandingByClass.entrySet()) {
                double fraction = billed <= 0 ? 0 : Math.max(0, Math.min(1, 1 - (entry.getValue() / billed)));
                rows.getChildren().add(Components.progressRow(entry.getKey(), fraction));
            }
        }

        card.getChildren().add(rows);
        return card;
    }

    private static double totalCollected() {
        try {
            return new com.school.sms.dao.PaymentDAO().totalCollected();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    // ------------------------------------------------------------------
    // Table
    // ------------------------------------------------------------------

    /**
     * The students behind the "not cleared" figure. Six weighted columns, money and
     * counts right-aligned in the tabular family, and two status columns that carry
     * their word as well as their tone.
     */
    private static Node studentsTable(User user, DashboardKpiService kpi) {
        DataTable table = new DataTable("Students not fully cleared",
                "Fees or requirements still outstanding",
                "View all",
                () -> StudentsNotClearedDetailScreen.show(MainShell.getStage(), user),
                DataTable.Column.of("Student", 1.5),
                DataTable.Column.of("Class", 0.8),
                DataTable.Column.numeric("Balance", 0.75),
                DataTable.Column.numeric("Age", 0.75),
                DataTable.Column.of("Requirements", 0.75),
                DataTable.Column.of("Status", 0.8));
        table.setPageSize(5);

        try {
            List<DataTable.Row> rows = new ArrayList<>();
            for (DashboardKpiService.NotClearedRow row : kpi.getStudentsNotClearedDetails()) {
                Student student = row.student;
                if (student == null) {
                    continue;
                }
                Components.Tone requirementsTone = row.requirementsIncomplete
                        ? Components.Tone.WARNING : Components.Tone.SUCCESS;
                Components.Tone statusTone = row.balance > 0 ? Components.Tone.DANGER : Components.Tone.SUCCESS;
                rows.add(DataTable.Row.of(
                        () -> StudentsNotClearedDetailScreen.show(MainShell.getStage(), user),
                        DataTable.Cell.name(student.getFullName()),
                        DataTable.Cell.text(text(student.getStudentClass())),
                        DataTable.Cell.money(row.balance),
                        DataTable.Cell.number(student.getAge() > 0 ? String.valueOf(student.getAge()) : "\u2014"),
                        DataTable.Cell.badge(row.requirementsIncomplete ? "Incomplete" : "Complete", requirementsTone),
                        DataTable.Cell.badge(row.balance > 0 ? "Balance due" : "Cleared", statusTone)));
            }
            if (rows.isEmpty()) {
                table.setEmptyState(Components.emptyState(FontAwesomeSolid.CHECK_CIRCLE,
                        "Every student is fully cleared",
                        "No student currently has fee or requirement balances outstanding.",
                        "Review requirements", () -> RequirementsScreen.show(MainShell.getStage(), user)));
            }
            table.setRows(rows);
        } catch (Throwable failure) {
            table.setEmptyState(Components.emptyState(FontAwesomeSolid.EXCLAMATION_TRIANGLE,
                    "The list could not be loaded",
                    "The database could not be read just now: " + failure.getMessage(),
                    "Open students list", () -> StudentsListScreen.show(MainShell.getStage(), user)));
        }
        return table.node();
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? "\u2014" : value;
    }

}
