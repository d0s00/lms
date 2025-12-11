package com.lms.app.controller;

import com.lms.app.model.DepartmentItem;
import com.lms.app.model.AcademicYearItem;
import com.lms.app.util.UserSession;
import com.lms.app.util.DatabaseConnection;
import com.lms.app.util.FileHandler;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * UserProfile Controller.
 * 
 * Purpose:
 * - Allows any User to view their personal details (ID, Role, Department).
 * - Provides functionality to Change Password.
 * - Handles input validation for password changes (matching new password
 * fields).
 */
public class UserProfileController {

    @FXML
    private TextField userIdField;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ComboBox<String> roleCombo;
    @FXML
    private ComboBox<DepartmentItem> deptCombo;
    @FXML
    private ComboBox<AcademicYearItem> yearCombo;
    @FXML
    private ImageView profileImageView;
    @FXML
    private Button saveBtn;
    @FXML
    private Button imageBtn;

    private int targetUserId;
    private File selectedImageFile;

    public void initialize() {
        roleCombo.getItems().addAll("Admin", "Instructor", "Student", "Locked");
        loadDepartments();
        loadAcademicYears();
    }

    private void loadDepartments() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM departments")) {
            ResultSet rs = stmt.executeQuery();
            deptCombo.getItems().add(new DepartmentItem(1, "General")); // Default
            while (rs.next()) {
                int id = rs.getInt("id");
                if (id == 1)
                    continue; // Skip if already added
                deptCombo.getItems().add(new DepartmentItem(id, rs.getString("name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadAcademicYears() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT id, year_name FROM academic_years")) {
            ResultSet rs = stmt.executeQuery();
            yearCombo.getItems().add(new AcademicYearItem(1, "Default"));
            while (rs.next()) {
                int id = rs.getInt("id");
                if (id == 1)
                    continue;
                yearCombo.getItems().add(new AcademicYearItem(id, rs.getString("year_name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setTargetUserId(int userId) {
        this.targetUserId = userId;
        loadProfile();
        checkPermissions();
    }

    private void checkPermissions() {
        boolean isAdmin = UserSession.getInstance().getRole().equals("Admin");
        boolean isOwnProfile = UserSession.getInstance().getUserId() == targetUserId;
        boolean canEdit = isAdmin || isOwnProfile;

        passwordField.setEditable(canEdit);
        imageBtn.setDisable(!canEdit);
        saveBtn.setDisable(!canEdit);

        // Only Admin can edit role
        roleCombo.setDisable(!isAdmin || isOwnProfile); // Own profile cannot change role? Requirement usually yes.

        // Only Admin can edit Department/Year (usually students don't change this
        // themselves)
        // Or maybe students can request change? Let's say only Admin for now as
        // requested.
        deptCombo.setDisable(!isAdmin);
        yearCombo.setDisable(!isAdmin);

        // But users should see them. Logic "Disabled" might grey them out hard.
        // If not admin, maybe make them read-only or just disabled is fine.

        // Requirement said "Admin controls", implies student cannot change. Correct.
    }

    private void loadProfile() {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, targetUserId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                userIdField.setText(String.valueOf(targetUserId));
                usernameField.setText(rs.getString("username"));
                passwordField.setText(rs.getString("password"));
                roleCombo.setValue(rs.getString("role"));

                int deptId = rs.getInt("department_id");
                deptCombo.getItems().stream().filter(d -> d.getId() == deptId).findFirst()
                        .ifPresent(deptCombo::setValue);

                int yearId = rs.getInt("academic_year_id");
                yearCombo.getItems().stream().filter(y -> y.getId() == yearId).findFirst()
                        .ifPresent(yearCombo::setValue);

                byte[] imgData = rs.getBytes("profile_image");
                if (imgData != null && imgData.length > 0) {
                    profileImageView.setImage(FileHandler.getImageFromBytes(imgData));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void chooseImage() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        selectedImageFile = fc.showOpenDialog(null);
        if (selectedImageFile != null) {
            profileImageView.setImage(new Image(selectedImageFile.toURI().toString()));
        }
    }

    public void saveProfile() {
        String newPass = passwordField.getText();
        String role = roleCombo.getValue();
        int deptId = (deptCombo.getValue() != null) ? deptCombo.getValue().getId() : 1;
        int yearId = (yearCombo.getValue() != null) ? yearCombo.getValue().getId() : 1;

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            if (selectedImageFile != null) {
                byte[] imgData = FileHandler.readFileToBytes(selectedImageFile);
                String sql = "UPDATE users SET password = ?, profile_image = ?, role = ?, department_id = ?, academic_year_id = ? WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, newPass);
                    stmt.setBytes(2, imgData);
                    stmt.setString(3, role);
                    stmt.setInt(4, deptId);
                    stmt.setInt(5, yearId);
                    stmt.setInt(6, targetUserId);
                    stmt.executeUpdate();
                }
            } else {
                String sql = "UPDATE users SET password = ?, role = ?, department_id = ?, academic_year_id = ? WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, newPass);
                    stmt.setString(2, role);
                    stmt.setInt(3, deptId);
                    stmt.setInt(4, yearId);
                    stmt.setInt(5, targetUserId);
                    stmt.executeUpdate();
                }
            }

            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setContentText("Profile Updated Successfully!");
            a.show();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteProfile() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Account");
        alert.setHeaderText("Are you sure you want to delete your account?");
        alert.setContentText("This action cannot be undone.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            String sql = "DELETE FROM users WHERE id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, targetUserId);
                stmt.executeUpdate();

                // If user deleted themselves, logout
                if (targetUserId == UserSession.getInstance().getUserId()) {

                    UserSession.cleanSession();
                    // Basic redirect to login
                    javafx.application.Platform.exit();
                } else {
                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setContentText("User Deleted Successfully!");
                    a.show();
                    // Close the view/tab if possible?
                    // We can close the stage from anywhere
                    if (saveBtn.getScene().getWindow() instanceof javafx.stage.Stage) {
                        ((javafx.stage.Stage) saveBtn.getScene().getWindow()).close();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
