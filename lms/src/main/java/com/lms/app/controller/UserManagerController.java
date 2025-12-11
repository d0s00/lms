package com.lms.app.controller;

import com.lms.app.model.User;
import com.lms.app.model.DepartmentItem;
import com.lms.app.model.AcademicYearItem;
import com.lms.app.util.DatabaseConnection;
import com.lms.app.util.AlertHelper;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;

/**
 * UserManager Controller.
 * 
 * Purpose:
 * - (Admin Only) User Account administration.
 * - Allows Admins to Create new users (Student/Instructor) with specific
 * Departments.
 * - Allows Admins to Lock/Unlock accounts (preventing login).
 * - Allows Admins to Reset User passwords.
 */
public class UserManagerController {

    @FXML
    private TableView<User> userTable;
    @FXML
    private TableColumn<User, Integer> idCol;
    @FXML
    private TableColumn<User, String> usernameCol;
    @FXML
    private TableColumn<User, String> roleCol;

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ComboBox<String> roleCombo;

    public void initialize() {
        idCol.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getId()));
        usernameCol.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUsername()));
        roleCol.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRole()));

        roleCombo.getItems().addAll("Admin", "Instructor", "Student", "Locked");
        roleCombo.getSelectionModel().select("Student");

        loadUsers();

        // Double click to edit
        userTable.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    openUserEditor(row.getItem());
                }
            });
            return row;
        });

        // Context Menu
        ContextMenu cm = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit User");
        editItem.setOnAction(e -> openUserEditor(userTable.getSelectionModel().getSelectedItem()));
        MenuItem deleteItem = new MenuItem("Delete User");
        deleteItem.setOnAction(e -> deleteSelectedUser());
        cm.getItems().addAll(editItem, deleteItem);
        userTable.setContextMenu(cm);
    }

    private void loadUsers() {
        ObservableList<User> list = FXCollections.observableArrayList();
        String sql = "SELECT * FROM users";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getBytes("profile_image"),
                        rs.getInt("department_id"),
                        rs.getInt("academic_year_id")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        userTable.setItems(list);
    }

    public void addUser() {
        String user = usernameField.getText();
        String pass = passwordField.getText();
        String role = roleCombo.getValue();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Error", "Username and Password required.");
            return;
        }

        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user);
            stmt.setString(2, pass);
            stmt.setString(3, role);
            stmt.executeUpdate();

            usernameField.clear();
            passwordField.clear();
            loadUsers();
        } catch (SQLException e) {
            showAlert("Error", "Could not add user. Username might be taken.");
            e.printStackTrace();
        }
    }

    @FXML
    private void editSelectedUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a user to edit.");
            return;
        }
        openUserEditor(selected);
    }

    @FXML
    private void deleteSelectedUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setContentText("Delete user " + selected.getUsername() + "?");
        if (alert.showAndWait().get() == ButtonType.OK) {

            try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
                conn.setAutoCommit(false); // Transaction
                try {
                    // 1. Delete Submissions (Student)
                    try (PreparedStatement stmt = conn
                            .prepareStatement("DELETE FROM submissions WHERE student_id = ?")) {
                        stmt.setInt(1, selected.getId());
                        stmt.executeUpdate();
                    }

                    // 2. Handle Instructor (Courses -> Modules -> Assignments -> Submissions)
                    // For simplicity, we might just UNASSIGN courses or Cascade Delete hard.
                    // Let's go with Cascade Delete to be clean, but complex.
                    // Step 2a: Find courses by this instructor
                    String getCourses = "SELECT id FROM courses WHERE instructor_id = ?";
                    try (PreparedStatement courseStmt = conn.prepareStatement(getCourses)) {
                        courseStmt.setInt(1, selected.getId());
                        ResultSet rs = courseStmt.executeQuery();
                        while (rs.next()) {
                            int courseId = rs.getInt("id");
                            // Delete modules for course
                            // (We need to delete assignments and submissions for those modules too...
                            // this gets very deep. simpler to just SET NULL for instructor_id if schema
                            // allows,
                            // OR delete course but rely on other cascades.
                            // Let's try deletion of course and rely on DB/Manual.
                            // Since we didn't add deep cascade code everywhere, let's just delete the
                            // course record
                            // and let the orphan modules stay or delete them?)

                            // BETTER: Just delete the course row. If foreign keys fail, we catch it.
                            // But wait, modules refer to course_id.
                            // Let's manually delete modules for this course.
                            String getMods = "SELECT id FROM modules WHERE course_id = ?";
                            try (PreparedStatement modStmt = conn.prepareStatement(getMods)) {
                                modStmt.setInt(1, courseId);
                                ResultSet modRs = modStmt.executeQuery();
                                while (modRs.next()) {
                                    int modId = modRs.getInt("id");
                                    // Delete assignments
                                    String getAss = "SELECT id FROM assignments WHERE module_id = ?";
                                    try (PreparedStatement assStmt = conn.prepareStatement(getAss)) {
                                        assStmt.setInt(1, modId);
                                        ResultSet assRs = assStmt.executeQuery();
                                        while (assRs.next()) {
                                            int assId = assRs.getInt("id");
                                            try (PreparedStatement subStmt = conn.prepareStatement(
                                                    "DELETE FROM submissions WHERE assignment_id = ?")) {
                                                subStmt.setInt(1, assId);
                                                subStmt.executeUpdate();
                                            }
                                        }
                                    }
                                    try (PreparedStatement delAss = conn
                                            .prepareStatement("DELETE FROM assignments WHERE module_id = ?")) {
                                        delAss.setInt(1, modId);
                                        delAss.executeUpdate();
                                    }
                                }
                            }
                            try (PreparedStatement delMod = conn
                                    .prepareStatement("DELETE FROM modules WHERE course_id = ?")) {
                                delMod.setInt(1, courseId);
                                delMod.executeUpdate();
                            }
                        }
                    }
                    try (PreparedStatement delCourse = conn
                            .prepareStatement("DELETE FROM courses WHERE instructor_id = ?")) {
                        delCourse.setInt(1, selected.getId());
                        delCourse.executeUpdate();
                    }

                    // 3. Finally delete User
                    String sql = "DELETE FROM users WHERE id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, selected.getId());
                        stmt.executeUpdate();
                    }

                    conn.commit();
                    loadUsers();
                } catch (SQLException e) {
                    conn.rollback();
                    e.printStackTrace();
                    showAlert("Error", "Could not delete user: " + e.getMessage());
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void openUserEditor(User user) {
        if (user == null)
            return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/app/UserProfileView.fxml"));
            Parent root = loader.load();

            UserProfileController controller = loader.getController();
            controller.setTargetUserId(user.getId());

            Stage stage = new Stage();
            stage.setTitle("Edit User: " + user.getUsername());
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setWidth(600);
            stage.setHeight(850);
            stage.setResizable(true);
            stage.showAndWait(); // Wait to refresh after edit
            loadUsers();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }
}
