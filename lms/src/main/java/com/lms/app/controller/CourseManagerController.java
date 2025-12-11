package com.lms.app.controller;

import com.lms.app.model.Course;
import com.lms.app.model.DepartmentItem;
import com.lms.app.model.AcademicYearItem;
import com.lms.app.util.UserSession;
import com.lms.app.util.DatabaseConnection;
import com.lms.app.util.FileHandler;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.*;

/**
 * CourseManager Controller.
 * 
 * Purpose:
 * - (Instructor Only) Interface for managing courses.
 * - Allows creating new Courses, Setting Titles, Descriptions, and Cover
 * Images.
 * - Displays a list of courses owned by the current instructor.
 * - Provides access to "Delete" functionality.
 */
public class CourseManagerController {

    @FXML
    private TableView<Course> courseTable;
    @FXML
    private TableColumn<Course, String> titleCol;
    @FXML
    private TableColumn<Course, String> descCol;

    @FXML
    private TextField titleField;
    @FXML
    private TextArea descField;
    @FXML
    private Button imageBtn;
    @FXML
    private ComboBox<DepartmentItem> departmentComboBox;
    @FXML
    private ComboBox<AcademicYearItem> academicYearComboBox;

    private File selectedImageFile;

    public void initialize() {
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        loadDepartments();
        loadAcademicYears();
        loadCourses();

        // Context Menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem manageModulesItem = new MenuItem("Manage Modules");
        manageModulesItem.setOnAction(e -> openModuleEditor(courseTable.getSelectionModel().getSelectedItem()));
        contextMenu.getItems().add(manageModulesItem);
        courseTable.setContextMenu(contextMenu);
    }

    private void loadCourses() {
        ObservableList<Course> list = FXCollections.observableArrayList();
        int userId = UserSession.getInstance().getUserId();
        String sql = "SELECT * FROM courses WHERE instructor_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new Course(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getInt("instructor_id"),
                        rs.getBytes("course_image")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        courseTable.setItems(list);
    }

    public void chooseImage() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        selectedImageFile = fc.showOpenDialog(null);
        if (selectedImageFile != null) {
            imageBtn.setText(selectedImageFile.getName());
        }
    }

    public void addCourse() {
        String title = titleField.getText();
        String desc = descField.getText();

        int departmentId = (departmentComboBox.getValue() != null) ? departmentComboBox.getValue().getId() : 1;
        int academicYearId = (academicYearComboBox.getValue() != null) ? academicYearComboBox.getValue().getId() : 1;

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            String sql = "INSERT INTO courses (title, description, instructor_id, course_image, department_id, academic_year_id) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, title);
                stmt.setString(2, desc);
                stmt.setInt(3, UserSession.getInstance().getUserId());

                if (selectedImageFile != null) {
                    stmt.setBytes(4, FileHandler.readFileToBytes(selectedImageFile));
                } else {
                    stmt.setBytes(4, null);
                }

                stmt.setInt(5, departmentId);
                stmt.setInt(6, academicYearId);

                stmt.executeUpdate();
                loadCourses();
                clearForm();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void clearForm() {
        titleField.clear();
        descField.clear();
        selectedImageFile = null;
        imageBtn.setText("Choose Cover Image");
    }

    @FXML
    public void deleteCourse() {
        Course selected = courseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setContentText("Please select a course to delete.");
            a.show();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete course '" + selected.getTitle() + "' and all its content?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Manual Cascade: Course -> Modules -> Assignments -> Submissions

                    // 1. Get Modules
                    String modSql = "SELECT id FROM modules WHERE course_id = ?";
                    try (PreparedStatement modStmt = conn.prepareStatement(modSql)) {
                        modStmt.setInt(1, selected.getId());
                        ResultSet modRs = modStmt.executeQuery();
                        while (modRs.next()) {
                            int modId = modRs.getInt("id");

                            // 2. Get Assignments for Module
                            String assSql = "SELECT id FROM assignments WHERE module_id = ?";
                            try (PreparedStatement assStmt = conn.prepareStatement(assSql)) {
                                assStmt.setInt(1, modId);
                                ResultSet assRs = assStmt.executeQuery();
                                while (assRs.next()) {
                                    int assId = assRs.getInt("id");

                                    // 3. Delete Submissions for Assignment
                                    try (PreparedStatement subDel = conn
                                            .prepareStatement("DELETE FROM submissions WHERE assignment_id = ?")) {
                                        subDel.setInt(1, assId);
                                        subDel.executeUpdate();
                                    }
                                }
                            }
                            // 4. Delete Assignments
                            try (PreparedStatement assDel = conn
                                    .prepareStatement("DELETE FROM assignments WHERE module_id = ?")) {
                                assDel.setInt(1, modId);
                                assDel.executeUpdate();
                            }
                        }
                    }
                    // 5. Delete Modules
                    try (PreparedStatement modDel = conn.prepareStatement("DELETE FROM modules WHERE course_id = ?")) {
                        modDel.setInt(1, selected.getId());
                        modDel.executeUpdate();
                    }

                    // 6. Delete Course
                    try (PreparedStatement courseDel = conn.prepareStatement("DELETE FROM courses WHERE id = ?")) {
                        courseDel.setInt(1, selected.getId());
                        courseDel.executeUpdate();
                    }

                    conn.commit();
                    loadCourses();

                    Alert success = new Alert(Alert.AlertType.INFORMATION, "Course deleted successfully.");
                    success.show();

                } catch (SQLException e) {
                    conn.rollback();
                    e.printStackTrace();
                    Alert err = new Alert(Alert.AlertType.ERROR, "Error deleting course: " + e.getMessage());
                    err.show();
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void openModuleEditor(Course course) {
        if (course == null)
            return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/app/ModuleEditor.fxml"));
            Parent root = loader.load();
            ModuleEditorController controller = loader.getController();
            controller.setCourseId(course.getId());

            Stage stage = new Stage();
            stage.setTitle("Manage Modules: " + course.getTitle());
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setWidth(900);
            stage.setHeight(700);
            stage.setResizable(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadDepartments() {
        departmentComboBox.getItems().clear();
        String sql = "SELECT * FROM departments ORDER BY id";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                departmentComboBox.getItems().add(
                        new DepartmentItem(rs.getInt("id"), rs.getString("name")));
            }

            if (!departmentComboBox.getItems().isEmpty()) {
                departmentComboBox.getSelectionModel().select(0);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadAcademicYears() {
        academicYearComboBox.getItems().clear();
        String sql = "SELECT * FROM academic_years WHERE is_active = TRUE ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                academicYearComboBox.getItems().add(
                        new AcademicYearItem(rs.getInt("id"), rs.getString("year_name")));
            }

            if (!academicYearComboBox.getItems().isEmpty()) {
                academicYearComboBox.getSelectionModel().select(0);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
