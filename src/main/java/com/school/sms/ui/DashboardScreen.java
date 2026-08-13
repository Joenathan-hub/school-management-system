package com.school.sms.ui;

import com.school.sms.model.User;
import javafx.stage.Stage;

/**
 * Entry point called right after login. Delegates to MainShell, which
 * builds the persistent sidebar + content area ONCE for this session.
 */
public class DashboardScreen {

    public static void show(Stage stage, User user) {
        MainShell.initialize(stage, user);
    }
}