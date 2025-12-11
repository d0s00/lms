package com.lms.app.controller;

import com.lms.app.util.UserSession;
import com.lms.app.util.DatabaseConnection;
import com.lms.app.util.AlertHelper;
import com.lms.app.model.User; // Added by user instruction

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.event.ActionEvent; // Added by user instruction

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Login Controller.
 * 
 * Purpose:
 * - Manages the authentication process.
 * - Validates user credentials against the database.
 * - Handles "Forgot Password" logic (if implemented) or Account Locking checks.
 * - Initializes the `UserSession` upon success.
 */
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    /**
     * Handles the "Login" button click event.
     * 
     * Logic Flow:
     * 1. Extracts the raw text from `usernameField` and `passwordField`.
     * 2. Checks if either field is empty; if so, shows an Error Alert.
     * 3. Calls `validateLogin(user, pass)` to check credentials against the DB.
     * 4. If valid:
     * - Loads the Dashboard using `loadDashboard()`.
     * - Closes specific login resources if needed.
     * 5. If invalid:
     * - Shows an "Invalid Credentials" error to the user.
     */
    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            AlertHelper.showError("Login Error", "Please enter both username and password.");
            return;
        }

        if (validateLogin(username, password)) {
            // Transition to the main application screen
            loadDashboard();
        } else {
            // Wait, user asked for "If role is locked, cannot login".
            // With my change, validateLogin returns false for Locked.
            // So it shows "Login Failed" - "Invalid credentials".
            // Ideally we want to say "Account Locked".
            // I will change validateLogin signature in next step if needed or assume
            // invalid is sufficient "prevention".
            // Actually, the user requirement is "Cannot login". Returning false satisfies
            // this.
            // I will improve the message by checking specific "Locked" query if needed,
            // But let's verify if I should just use a member variable or return int.
            showAlert("Login Failed", "Invalid credentials or Account Locked.");
        }
    }

    private boolean validateLogin(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");
                if ("Locked".equalsIgnoreCase(role)) {
                    return false; // Valid credentials but locked
                }

                UserSession.getInstance(
                        rs.getInt("id"),
                        rs.getString("username"),
                        role,
                        rs.getBytes("profile_image"),
                        rs.getInt("department_id"),
                        rs.getInt("academic_year_id"));
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void loadDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/lms/app/DashboardView.fxml"));
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("LMS Dashboard - " + UserSession.getInstance().getUsername());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        AlertHelper.showError(title, content);
    }
}
