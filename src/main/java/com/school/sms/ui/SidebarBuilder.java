package com.school.sms.ui;

import com.school.sms.model.Role;
import com.school.sms.model.User;
import com.school.sms.service.BackupService;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SidebarBuilder {

    public static class SidebarResult {
        public final VBox container;
        public final Button dashboardHomeButton;

        public SidebarResult(VBox container, Button dashboardHomeButton) {
            this.container = container;
            this.dashboardHomeButton = dashboardHomeButton;
        }
    }

    public static VBox buildHeader(User user) {
        VBox header = new VBox(2);
        header.getStyleClass().add("side-nav-header");

        Label sidebarTitle = new Label(user.getFullName());
        sidebarTitle.getStyleClass().add("side-nav-title");
        Label sidebarSubtitle = new Label(user.getRole().toString() +
                (user.getAssignedClass() != null && !user.getAssignedClass().isBlank() ? " — " + user.getAssignedClass() : ""));
        sidebarSubtitle.getStyleClass().add("side-nav-subtitle");

        header.getChildren().addAll(sidebarTitle, sidebarSubtitle, spacer(6), new Separator());
        return header;
    }

    public static SidebarResult buildNavButtons(Stage stage, User user) {
        VBox buttons = new VBox(10);
        buttons.getStyleClass().add("side-nav-buttons");

        Button dashboardHomeBtn = navButton("Dashboard Home", MainShell::showHome);
        buttons.getChildren().add(dashboardHomeBtn);

        if (user.getRole() == Role.ADMINISTRATOR || user.getRole() == Role.BURSAR) {
            buttons.getChildren().add(navButton("Admit New Student", () -> AdmissionFormScreen.show(stage, user)));
            buttons.getChildren().add(navButton("Students List", () -> StudentsListScreen.show(stage, user)));
            buttons.getChildren().add(navButton("Record Student Payment", () -> PaymentScreen.show(stage, user)));
            buttons.getChildren().add(navButton("Treasury Dashboard", () -> TreasuryScreen.show(stage, user)));
            buttons.getChildren().add(navButton("Manage / Pay Workers", () -> WorkerScreen.show(stage, user)));
            buttons.getChildren().add(navButton("Uniform Checklist", () -> UniformScreen.show(stage, user)));
            buttons.getChildren().add(navButton("Requirements Checklist", () -> RequirementsScreen.show(stage, user)));
            buttons.getChildren().add(navButton("View / Print Report Card", () -> ReportCardScreen.show(stage, user)));
            buttons.getChildren().add(navButton("Search Students", () -> AdvancedSearchScreen.show(stage, user)));
            buttons.getChildren().add(navButton("Set Term Fees", () -> FeeStructureScreen.show(stage, user)));
        }

        if (user.getRole() == Role.BURSAR) {
            buttons.getChildren().add(navButton("Create Teacher Account", () -> TeacherAccountScreen.show(stage, user)));
        }

        if (user.getRole() == Role.TEACHER) {
            buttons.getChildren().add(navButton("Enter Results", () -> ResultsEntryScreen.show(stage, user)));
            buttons.getChildren().add(navButton("Attendance / Return Confirmation", () -> AttendanceScreen.show(stage, user)));
        }

        if (user.getRole() == Role.ADMINISTRATOR) {
            buttons.getChildren().add(navButton("Manage User Accounts", () -> UserManagementScreen.show(stage, user)));
            buttons.getChildren().add(navButton("Term Management", () -> TermManagementScreen.show(stage, user)));
            buttons.getChildren().add(navButton("Manage Requirement Lists", () -> ManageRequirementTemplatesScreen.show(stage, user)));
            buttons.getChildren().add(navButton("Audit Log", () -> AuditLogScreen.show(stage, user)));
            buttons.getChildren().add(navButton("Manage School Events", () -> ManageEventsScreen.show(stage, user)));
            buttons.getChildren().add(navButton("Check for Updates", () -> UpdateScreen.show(stage, user)));
            buttons.getChildren().add(navButton("Backup Database", () -> runBackup(buttons)));
        }

        VBox spacerGrow = new VBox();
        VBox.setVgrow(spacerGrow, Priority.ALWAYS);
        buttons.getChildren().add(spacerGrow);

        Button themeToggle = new Button(com.school.sms.util.ThemeManager.isDarkMode() ? "Switch to Light Mode" : "Switch to Dark Mode");
        themeToggle.getStyleClass().add("side-nav-theme-toggle");
        themeToggle.setMaxWidth(Double.MAX_VALUE);
        themeToggle.setOnAction(e -> {
            com.school.sms.util.ThemeManager.setDarkMode(!com.school.sms.util.ThemeManager.isDarkMode());
            com.school.sms.util.ThemeManager.applyTheme(stage.getScene());
            themeToggle.setText(com.school.sms.util.ThemeManager.isDarkMode() ? "Switch to Light Mode" : "Switch to Dark Mode");
        });
        buttons.getChildren().add(themeToggle);

        Button logout = new Button("Log Out");
        logout.getStyleClass().add("side-nav-logout");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(e -> com.school.sms.Main.showLogin(stage));
        buttons.getChildren().add(logout);

        return new SidebarResult(buttons, dashboardHomeBtn);
    }

    private static void runBackup(VBox buttons) {
        BackupService backupService = new BackupService();
        Label status = new Label();
        status.getStyleClass().add("side-nav-subtitle");
        status.setWrapText(true);
        try {
            backupService.backupNow();
            status.setText("Backup saved. Last backup: " + backupService.getLastBackupTime());
        } catch (Exception ex) {
            status.setText("Backup failed: " + ex.getMessage());
        }
        buttons.getChildren().add(status);
    }

    private static Button navButton(String label, Runnable action) {
        Button b = new Button(label);
        b.getStyleClass().add("side-nav-button");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> {
            MainShell.setActiveButton(b);
            action.run();
        });
        return b;
    }

    private static VBox spacer(double height) {
        VBox v = new VBox();
        v.setPrefHeight(height);
        return v;
    }
}