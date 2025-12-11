package com.lms.app.controller;

import com.lms.app.util.AlertHelper;
import com.lms.app.util.DatabaseConnection;
import com.lms.app.util.FileHandler;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.File;
import java.awt.Desktop;
import java.io.IOException;
import java.sql.*;

/**
 * Grading Controller.
 * 
 * Purpose:
 * - (Instructor Only) Interface for grading student submissions.
 * - Displays a table of submissions with student names, dates, and files.
 * - Allows the instructor to enter a numeric score and add feedback.
 * - Updates the `submissions` table in the database.
 */
public class GradingController {

    @FXML
    private TableView<SubmissionDTO> pendingTable;
    @FXML
    private TableColumn<SubmissionDTO, String> studentCol;
    @FXML
    private TableColumn<SubmissionDTO, String> assignCol;
    @FXML
    private TableColumn<SubmissionDTO, String> dateCol;

    @FXML
    private TableView<SubmissionDTO> gradedTable;
    @FXML
    private TableColumn<SubmissionDTO, String> gStudentCol;
    @FXML
    private TableColumn<SubmissionDTO, String> gAssignCol;
    @FXML
    private TableColumn<SubmissionDTO, String> gDateCol;
    @FXML
    private TableColumn<SubmissionDTO, Integer> gScoreCol;

    @FXML
    private TextField scoreField;
    @FXML
    private TextArea feedbackArea;

    // Search Fields
    @FXML
    private TextField searchIdField;
    @FXML
    private Label studentInfoLabel;

    private Integer currentStudentFilterId = null;

    public void initialize() {
        // Pending Table
        studentCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        assignCol.setCellValueFactory(new PropertyValueFactory<>("assignmentTitle"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));

        // Graded Table
        gStudentCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        gAssignCol.setCellValueFactory(new PropertyValueFactory<>("assignmentTitle"));
        gDateCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        gScoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));

        loadSubmissions();

        // Listeners
        pendingTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                gradedTable.getSelectionModel().clearSelection();
                populateFields(newVal);
            }
        });

        gradedTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                pendingTable.getSelectionModel().clearSelection();
                populateFields(newVal);
            }
        });
    }

    private void populateFields(SubmissionDTO sub) {
        if (sub.getScore() != -1) {
            scoreField.setText(String.valueOf(sub.getScore()));
        } else {
            scoreField.clear();
        }
        feedbackArea.setText(sub.getFeedback() != null ? sub.getFeedback() : "");
    }

    private void loadSubmissions() {
        ObservableList<SubmissionDTO> pendingList = FXCollections.observableArrayList();
        ObservableList<SubmissionDTO> gradedList = FXCollections.observableArrayList();

        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT s.id, u.username, a.description, a.due_date, s.submission_data, s.file_type, s.score, s.feedback_text "
                        + "FROM submissions s "
                        + "JOIN assignments a ON s.assignment_id = a.id "
                        + "JOIN users u ON s.student_id = u.id ");

        if (currentStudentFilterId != null) {
            sqlBuilder.append("WHERE s.student_id = ? ");
        }

        String sql = sqlBuilder.toString();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (currentStudentFilterId != null) {
                stmt.setInt(1, currentStudentFilterId);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int scoreVal = rs.getInt("score");
                if (rs.wasNull())
                    scoreVal = -1;

                SubmissionDTO dto = new SubmissionDTO(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("description"),
                        rs.getString("due_date"),
                        rs.getBytes("submission_data"),
                        rs.getString("file_type"),
                        scoreVal,
                        rs.getString("feedback_text"));

                if (scoreVal == -1) {
                    pendingList.add(dto);
                } else {
                    gradedList.add(dto);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        pendingTable.setItems(pendingList);
        gradedTable.setItems(gradedList);
    }

    private SubmissionDTO getSelectedSubmission() {
        SubmissionDTO selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected != null)
            return selected;
        return gradedTable.getSelectionModel().getSelectedItem();
    }

    public void openStudentFile() {
        SubmissionDTO selected = getSelectedSubmission();
        if (selected != null && selected.getSubmissionData() != null) {
            try {
                String extension = selected.getFileType();
                if (extension == null || extension.isEmpty())
                    extension = "pdf";

                File tempFile = FileHandler.writeBytesToTempFile(selected.getSubmissionData(),
                        "submission_" + selected.getId() + "." + extension);

                if (tempFile != null && tempFile.exists()) {
                    Desktop.getDesktop().open(tempFile);
                } else {
                    AlertHelper.showError("Error", "Could not create temp file.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void submitGrade() {
        SubmissionDTO selected = getSelectedSubmission();
        if (selected == null)
            return;

        try {
            int score = Integer.parseInt(scoreField.getText());
            String feedback = feedbackArea.getText();

            String sql = "UPDATE submissions SET score = ?, feedback_text = ? WHERE id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, score);
                stmt.setString(2, feedback);
                stmt.setInt(3, selected.getId());
                stmt.executeUpdate();

                loadSubmissions(); // Refresh
                scoreField.clear();
                feedbackArea.clear();
            }
        } catch (NumberFormatException e) {
            AlertHelper.showError("Invalid Input", "Score must be a number.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void searchStudent() {
        String idText = searchIdField.getText().trim();
        if (idText.isEmpty()) {
            AlertHelper.showError("Error", "Please enter a Student ID");
            return;
        }

        try {
            int studentId = Integer.parseInt(idText);

            String sql = "SELECT u.username, d.name as dept_name, ay.year_name " +
                    "FROM users u " +
                    "LEFT JOIN departments d ON u.department_id = d.id " +
                    "LEFT JOIN academic_years ay ON u.academic_year_id = ay.id " +
                    "WHERE u.id = ? AND u.role = 'Student'";

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, studentId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String name = rs.getString("username");
                    String dept = rs.getString("dept_name");
                    String year = rs.getString("year_name");

                    studentInfoLabel.setText(String.format("ID: %d | Name: %s | Dept: %s | Year: %s",
                            studentId, name, dept != null ? dept : "N/A", year != null ? year : "N/A"));

                    currentStudentFilterId = studentId;
                    loadSubmissions();

                } else {
                    AlertHelper.showError("Not Found", "No student found with ID: " + studentId);
                    studentInfoLabel.setText("Student not found");
                    currentStudentFilterId = null;
                    loadSubmissions(); // Load all or clear? Usually reset checks valid ID.
                }
            }

        } catch (NumberFormatException e) {
            AlertHelper.showError("Error", "ID must be a number");
        } catch (SQLException e) {
            e.printStackTrace();
            AlertHelper.showError("Database Error", "Failed to search student");
        }
    }

    public void resetFilter() {
        searchIdField.clear();
        studentInfoLabel.setText("No student selected");
        currentStudentFilterId = null;
        loadSubmissions();
    }

    // DTO Inner Class
    public static class SubmissionDTO {
        private int id;
        private String username;
        private String assignmentTitle;
        private String dueDate;
        private byte[] submissionData;
        private String fileType;
        private int score;
        private String feedback;

        public SubmissionDTO(int id, String username, String assignmentTitle, String dueDate, byte[] submissionData,
                String fileType, int score, String feedback) {
            this.id = id;
            this.username = username;
            this.assignmentTitle = assignmentTitle;
            this.dueDate = dueDate;
            this.submissionData = submissionData;
            this.fileType = fileType;
            this.score = score;
            this.feedback = feedback;
        }

        public int getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getAssignmentTitle() {
            return assignmentTitle;
        }

        public String getDueDate() {
            return dueDate;
        }

        public byte[] getSubmissionData() {
            return submissionData;
        }

        public String getFileType() {
            return fileType;
        }

        public int getScore() {
            return score;
        }

        public String getFeedback() {
            return feedback;
        }
    }
}
