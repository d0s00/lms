package com.lms.app.controller;

import com.lms.app.model.Module;
import com.lms.app.model.*;
import com.lms.app.util.*;

import javafx.fxml.FXML;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.awt.Desktop;
import java.sql.*;

/**
 * CourseViewer Controller.
 * 
 * Purpose:
 * - Displays detailed information for a single Course.
 * - Shows the Course Title, Instructor, and a list of Modules.
 * - Allows Students to Download Module files and Upload Assignment solutions.
 * - Allows Instructors to Open the `ModuleEditor` to manage content.
 */
public class CourseViewerController {

    @FXML
    private Label courseTitleLbl;
    @FXML
    private Label instructorNameLbl;
    @FXML
    private ImageView courseImage;
    @FXML
    private Button editModulesBtn;
    @FXML
    private VBox modulesContainer;

    private Course course;

    public void setCourse(Course course) {
        this.course = course;
        courseTitleLbl.setText(course.getTitle());
        loadInstructorName();

        // Load course cover image if available
        if (course.getCourseImage() != null) {
            try {
                courseImage.setImage(FileHandler.getImageFromBytes(course.getCourseImage()));
            } catch (Exception e) {
                System.err.println("Error loading course image: " + e.getMessage());
            }
        }

        // Check role
        if ("Instructor".equalsIgnoreCase(UserSession.getInstance().getRole())) {
            editModulesBtn.setVisible(true);
            editModulesBtn.setManaged(true);
            editModulesBtn.setOnAction(e -> openModuleEditor());
        }

        loadModules();
    }

    private void loadInstructorName() {
        String sql = "SELECT username FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, course.getInstructorId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                instructorNameLbl.setText("Instructor: " + rs.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadModules() {
        String sql = "SELECT * FROM modules WHERE course_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, course.getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Module mod = new Module(
                        rs.getInt("id"),
                        rs.getInt("course_id"),
                        rs.getString("title"),
                        rs.getBytes("module_data"),
                        rs.getString("file_type"),
                        rs.getDate("upload_date") != null ? rs.getDate("upload_date").toLocalDate() : null);
                modulesContainer.getChildren().add(createModuleItem(mod));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox createModuleItem(Module mod) {
        VBox box = new VBox(5);
        box.setStyle("-fx-border-color: #ddd; -fx-border-width: 1; -fx-padding: 10; -fx-background-color: white;");

        Label title = new Label("Module: " + mod.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        String dateStr = "N/A";
        if (mod.getUploadDate() != null) {
            dateStr = mod.getUploadDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"));
        }

        Label dateLbl = new Label("Uploaded: " + dateStr);
        dateLbl.setStyle("-fx-text-fill: #444; -fx-font-size: 12px; -fx-font-style: italic;");

        Button downloadBtn = new Button("View File");
        downloadBtn.setOnAction(e -> openFile(mod));

        box.getChildren().addAll(title, dateLbl, downloadBtn);

        // Load assignments for this module
        loadAssignments(mod.getId(), box);

        return box;
    }

    private void loadAssignments(int moduleId, VBox container) {
        String sql = "SELECT * FROM assignments WHERE module_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, moduleId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int assignId = rs.getInt("id");
                String desc = rs.getString("description");
                int maxScore = rs.getInt("max_score");
                Date dueDate = rs.getDate("due_date");
                boolean hasFile = rs.getBytes("assignment_data") != null;
                String fileType = rs.getString("file_type");

                VBox assignBox = new VBox(5);
                assignBox.setStyle("-fx-border-color: #ddd; -fx-padding: 10; -fx-background-color: #f9f9f9;");

                Label descLbl = new Label("Assignment: " + desc);
                descLbl.setStyle("-fx-font-weight: bold;");
                Label scoreLbl = new Label("Max Score: " + maxScore + " | Due: " + dueDate);

                HBox actionBox = new HBox(10);

                if (hasFile) {
                    Button downloadBtn = new Button(
                            "Download Instructions (" + (fileType != null ? fileType.toUpperCase() : "FILE") + ")");
                    downloadBtn.setOnAction(e -> downloadAssignmentFile(assignId, fileType));
                    actionBox.getChildren().add(downloadBtn);
                }

                Button uploadBtn = new Button("Upload Solution");
                uploadBtn.setOnAction(e -> uploadSolution(assignId));
                actionBox.getChildren().add(uploadBtn);

                assignBox.getChildren().addAll(descLbl, scoreLbl, actionBox);
                container.getChildren().add(assignBox);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void downloadAssignmentFile(int assignId, String fileType) {
        String sql = "SELECT assignment_data FROM assignments WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, assignId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                byte[] data = rs.getBytes("assignment_data");
                if (data != null) {
                    File temp = FileHandler.writeBytesToTempFile(data,
                            "assignment_" + assignId + "." + (fileType != null ? fileType : "dat"));
                    if (temp != null) {
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().open(temp);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openFile(Module mod) {
        if (mod.getModuleData() == null)
            return;
        try {
            String extension = mod.getFileType();
            if (extension == null || extension.isEmpty())
                extension = "dat"; // Default

            File tempFile = FileHandler.writeBytesToTempFile(mod.getModuleData(), mod.getTitle() + "." + extension);
            if (tempFile != null && tempFile.exists()) {
                Desktop.getDesktop().open(tempFile);
            } else {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setContentText("Could not create temp file.");
                a.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uploadSolution(int assignmentId) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Solution File");
        File file = fc.showOpenDialog(null);

        if (file != null) {
            String sql = "INSERT INTO submissions (assignment_id, student_id, submission_data, file_type) VALUES (?, ?, ?, ?)";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, assignmentId);
                stmt.setInt(2, UserSession.getInstance().getUserId());
                stmt.setBytes(3, FileHandler.readFileToBytes(file));
                String fileName = file.getName();
                stmt.setString(4, fileName.substring(fileName.lastIndexOf(".") + 1));
                stmt.executeUpdate();

                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setContentText("Submitted successfully!");
                a.show();
            } catch (SQLException e) {
                e.printStackTrace();
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setContentText("Error uploading: " + e.getMessage());
                a.show();
            }
        }
    }

    private void openModuleEditor() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/lms/app/ModuleEditor.fxml"));
            javafx.scene.Parent root = loader.load();

            ModuleEditorController controller = loader.getController();
            controller.setCourseId(course.getId());

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Edit Modules - " + course.getTitle());
            stage.setScene(new javafx.scene.Scene(root));
            stage.setWidth(900);
            stage.setHeight(700);
            stage.setResizable(true);
            stage.showAndWait();

            // Refresh modules after editing
            modulesContainer.getChildren().clear();
            loadModules();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    private void handleClose() {
        if (courseTitleLbl.getScene() != null && courseTitleLbl.getScene().getWindow() != null) {
            ((javafx.stage.Stage) courseTitleLbl.getScene().getWindow()).close();
        }
    }
}
