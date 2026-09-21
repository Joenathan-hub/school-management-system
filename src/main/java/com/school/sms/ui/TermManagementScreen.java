package com.school.sms.ui;

import com.school.sms.model.Term;
import com.school.sms.model.User;
import com.school.sms.service.TermService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class TermManagementScreen {

    private static final DateTimeFormatter DATE_FMT = com.school.sms.util.DateTimeUtil.DATE_TIME;

    public static void show(Stage stage, User user) {
        TermService termService = new TermService();

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        VBox statusCard = new VBox(8);
        statusCard.getStyleClass().add("card");
        Label statusTitle = new Label("Current Term");
        statusTitle.getStyleClass().add("section-label");

        Term active = termService.getActiveTerm();
        Label activeLabel = new Label(active != null
                ? "Active: " + active.getName() + " (started " + active.getStartDate().format(DATE_FMT) + ")"
                : "No term has been started yet.");
        activeLabel.getStyleClass().add("field-label");

        statusCard.getChildren().addAll(statusTitle, activeLabel);

        VBox startCard = new VBox(8);
        startCard.getStyleClass().add("card");
        Label startTitle = new Label("Start New Term");
        startTitle.getStyleClass().add("section-label");

        Label warning = new Label("Starting a new term closes the current one, carries every active student's " +
                "outstanding fee balance forward into the new term, and re-registers their requirements checklist. " +
                "This cannot be undone from within the app.");
        warning.setWrapText(true);
        warning.getStyleClass().add("field-label");

        TextField newTermField = new TextField();
        newTermField.setPromptText("New term name (e.g. Term 2 2026)");

        DatePicker startDateField = new DatePicker(java.time.LocalDate.now());
        Label startDateLabel = new Label("Date term started:");
        startDateLabel.getStyleClass().add("field-label");

        Button startBtn = new Button("Start New Term");
        startBtn.getStyleClass().add("primary-button");
        Label startStatus = new Label();

        startBtn.setOnAction(e -> {
            String name = newTermField.getText().trim();
            if (name.isBlank()) {
                startStatus.getStyleClass().setAll("status-error");
                startStatus.setText("Enter a name for the new term.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Start \"" + name + "\"? This will close the current term and roll every student's balance and requirements forward.",
                    ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    TermService.RolloverSummary summary = termService.startNewTerm(name, user, startDateField.getValue());
                    startStatus.getStyleClass().setAll("status-success");
                    startStatus.setText(String.format(
                            "%s started. %d student(s) processed, UGX %,.0f carried forward from %s.",
                            summary.newTermName, summary.studentsProcessed, summary.totalBalanceCarriedForward,
                            summary.previousTermName != null ? summary.previousTermName : "no previous term"));
                    newTermField.clear();
                }
            });
        });

        startCard.getChildren().addAll(startTitle, warning, newTermField, startDateLabel, startDateField, startBtn, startStatus);

        VBox historyCard = new VBox(8);
        historyCard.getStyleClass().add("card");
        Label historyTitle = new Label("Term History");
        historyTitle.getStyleClass().add("section-label");

        ListView<Term> historyList = new ListView<>();
        historyList.setPrefHeight(160);
        historyList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Term t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) { setText(null); return; }
                String range = t.getStartDate() != null ? t.getStartDate().format(DATE_FMT) : "?";
                range += " → " + (t.getEndDate() != null ? t.getEndDate().format(DATE_FMT) : "present");
                setText(t.getName() + " [" + t.getStatus() + "] " + range);
            }
        });
        List<Term> allTerms = termService.getAllTerms();
        historyList.getItems().setAll(allTerms);

        historyCard.getChildren().addAll(historyTitle, historyList);

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        root.getChildren().addAll(statusCard, startCard, historyCard, back);
        MainShell.setCenter(root, "Term Management");
    }
}
