package com.school.sms.ui;

import com.school.sms.dao.RequirementTemplateDAO;
import com.school.sms.model.RequirementTemplate;
import com.school.sms.model.User;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Administrator-only: define separate requirement checklists for Boarding
 * and Day students. Whichever list applies is chosen automatically per
 * student based on the boarding status set at admission.
 */
public class ManageRequirementTemplatesScreen {

    public static void show(Stage stage, User user) {
        RequirementTemplateDAO templateDAO = new RequirementTemplateDAO();

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        root.getChildren().add(categoryCard("Boarding Students", "Boarding", templateDAO));
        root.getChildren().add(categoryCard("Day Students", "Day", templateDAO));

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());
        root.getChildren().add(back);

        MainShell.setCenter(root, "Manage Requirement Lists");
    }

    private static VBox categoryCard(String title, String category, RequirementTemplateDAO templateDAO) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-label");

        ListView<RequirementTemplate> list = new ListView<>();
        list.setPrefHeight(160);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(RequirementTemplate t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null : t.getItemName());
            }
        });
        list.getItems().setAll(templateDAO.findByCategory(category));

        TextField newItemField = new TextField();
        newItemField.setPromptText("New item name (e.g. Mattress)");
        Button addBtn = new Button("Add Item");
        addBtn.getStyleClass().add("primary-button");
        Button removeBtn = new Button("Remove Selected");
        removeBtn.getStyleClass().add("danger-button");
        Label status = new Label();

        addBtn.setOnAction(e -> {
            String name = newItemField.getText().trim();
            if (name.isBlank()) {
                status.getStyleClass().setAll("status-error");
                status.setText("Enter an item name first.");
                return;
            }
            templateDAO.insert(category, name);
            list.getItems().setAll(templateDAO.findByCategory(category));
            newItemField.clear();
            status.getStyleClass().setAll("status-success");
            status.setText("Added.");
        });

        removeBtn.setOnAction(e -> {
            RequirementTemplate selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) {
                status.getStyleClass().setAll("status-error");
                status.setText("Select an item to remove first.");
                return;
            }
            templateDAO.delete(selected.getId());
            list.getItems().setAll(templateDAO.findByCategory(category));
            status.getStyleClass().setAll("status-success");
            status.setText("Removed.");
        });

        card.getChildren().addAll(titleLabel, list, newItemField, new HBox(10, addBtn, removeBtn), status);
        return card;
    }
}