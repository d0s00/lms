package com.lms.app.controller;

import com.lms.app.util.UserSession;
import com.lms.app.util.DatabaseConnection;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StudentGradesController {

    @FXML
    private Label totalScoreLbl;

    @FXML
    private TableView<GradeItem> gradesTable;
    @FXML
    private TableColumn<GradeItem, String> courseCol;
    @FXML
    private TableColumn<GradeItem, String> assignmentCol;
    @FXML
    private TableColumn<GradeItem, String> scoreCol;
    @FXML
    private TableColumn<GradeItem, String> maxScoreCol;
    @FXML
    private TableColumn<GradeItem, String> feedbackCol;

    public void initialize() {
        courseCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().course));
        assignmentCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().assignment));
        scoreCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().score));
        maxScoreCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().maxScore));
        feedbackCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().feedback));

        loadGrades();
    }

    private void loadGrades() {
        int studentId = UserSession.getInstance().getUserId();
        ObservableList<GradeItem> list = FXCollections.observableArrayList();
        int totalEarned = 0;
        int totalPossible = 0;

        // Query to join submissions -> assignments -> modules -> courses
        // We want ALL assignments even if not submitted?
        // User asked for "Student grades in all assignments".
        // If not submitted, score is 0 or "Pending".
        // Let's typically show what they HAVE submitted or what is graded.
        // Or show everything. Let's show everything assigned to modules in courses the
        // student might be in?
        // Actually, there is no direct link "Student -> Course" table yet (enrollment).
        // The system is simple: All students see all courses currently
        // (StudentCatalog).
        // So let's just query SUBMISSIONS for this student. If they haven't submitted,
        // they don't have a grade yet.
        // Wait, if they haven't submitted, they might want to know they have 0/100.
        // But without an Enrollment table, we don't know which courses they are "in".
        // So we will stick to: List all assignments where they have a submission record
        // OR just list submissions.
        // Listing submissions is safest given current architecture.

        String sql = "SELECT c.title as course_title, a.description as assign_name, a.max_score, s.score, s.feedback_text "
                +
                "FROM submissions s " +
                "JOIN assignments a ON s.assignment_id = a.id " +
                "JOIN modules m ON a.module_id = m.id " +
                "JOIN courses c ON m.course_id = c.id " +
                "WHERE s.student_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String cTitle = rs.getString("course_title");
                String aName = rs.getString("assign_name");
                int max = rs.getInt("max_score");
                // Score can be null if not graded yet
                int scoreVal = rs.getInt("score");
                boolean isGraded = rs.getObject("score") != null;

                String scoreStr = isGraded ? String.valueOf(scoreVal) : "Pending";
                String feedback = rs.getString("feedback_text");
                if (feedback == null)
                    feedback = "-";

                list.add(new GradeItem(cTitle, aName, scoreStr, String.valueOf(max), feedback));

                if (isGraded) {
                    totalEarned += scoreVal;
                    totalPossible += max;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        gradesTable.setItems(list);
        totalScoreLbl.setText(String.format("Total Score: %d / %d", totalEarned, totalPossible));
    }

    public static class GradeItem {
        String course;
        String assignment;
        String score;
        String maxScore;
        String feedback;

        public GradeItem(String c, String a, String s, String m, String f) {
            this.course = c;
            this.assignment = a;
            this.score = s;
            this.maxScore = m;
            this.feedback = f;
        }
    }
}
