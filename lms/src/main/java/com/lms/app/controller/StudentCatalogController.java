package com.lms.app.controller;

import com.lms.app.model.Course;
import com.lms.app.util.UserSession;
import com.lms.app.util.DatabaseConnection;
import com.lms.app.util.FileHandler;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * StudentCatalog Controller.
 * 
 * Purpose:
 * - (Student Only) Displays all available courses in the system.
 * - Allows Students to browse and find courses they might want to take.
 * - (Future Scope) Handles course enrollment logic.
 */
public class StudentCatalogController {

    @FXML
    private FlowPane courseGrid;

    public void initialize() {
        loadCourses();
    }

    private void loadCourses() {
        int deptId = UserSession.getInstance().getDepartmentId();
        int yearId = UserSession.getInstance().getAcademicYearId();

        // Filter: Courses for (Student's Dept OR General) AND (Student's Year OR Active
        // Years)
        // Adjust logic: Usually specific courses are tied to specific years.
        // Assuming courses with dept_id=1 are General (avail to all)
        // Assuming courses with academic_year_id=1 are Default (avail to all, or handle
        // as 'Current')

        String sql = "SELECT * FROM courses WHERE " +
                "(department_id = ? OR department_id = 1) " +
                "AND (academic_year_id = ? OR academic_year_id = 1)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, deptId);
            stmt.setInt(2, yearId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Course course = new Course(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getInt("instructor_id"),
                        rs.getBytes("course_image"));
                courseGrid.getChildren().add(createCourseCard(course));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox createCourseCard(Course course) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
        card.setPrefWidth(200);
        card.setAlignment(Pos.CENTER);

        ImageView imgView = new ImageView();
        imgView.setFitWidth(180);
        imgView.setFitHeight(120);
        imgView.setPreserveRatio(true);

        if (course.getCourseImage() != null) {
            imgView.setImage(FileHandler.getImageFromBytes(course.getCourseImage()));
        } else {
            // Placeholder logic or empty
        }

        Label titleLbl = new Label(course.getTitle());
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Button openBtn = new Button("Open Course");
        openBtn.setOnAction(e -> openCourseViewer(course));

        card.getChildren().addAll(imgView, titleLbl, openBtn);
        return card;
    }

    private void openCourseViewer(Course course) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/app/CourseViewer.fxml"));
            Parent root = loader.load();

            CourseViewerController controller = loader.getController();
            controller.setCourse(course);

            Stage stage = new Stage();
            stage.setTitle(course.getTitle());
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
}
