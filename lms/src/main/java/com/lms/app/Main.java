package com.lms.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.lms.app.util.DatabaseConnection;

/**
 * Main Entry Point for the LMS Application.
 * 
 * Purpose:
 * - Initializes the JavaFX runtime.
 * - Sets up the primary Stage (window).
 * - Loads the initial LoginView (`LoginView.fxml`).
 * - Triggers database schema patching (`SchemaPatcher`) to ensure the DB is
 * ready on startup.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/lms/app/LoginView.fxml"));
        primaryStage.setTitle("LMS Login");
        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // TEMP MIGRATION: Ensure submissions table exists
        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
                java.sql.Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS submissions (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "assignment_id INT, " +
                    "student_id INT, " +
                    "submission_data LONGBLOB, " +
                    "file_type VARCHAR(10), " +
                    "score INT DEFAULT NULL, " +
                    "feedback_text TEXT, " +
                    "FOREIGN KEY (assignment_id) REFERENCES assignments (id), " +
                    "FOREIGN KEY (student_id) REFERENCES users (id))";
            stmt.executeUpdate(sql);

            // Fix: Attempt to add columns if they are missing (for existing tables)
            try {
                stmt.executeUpdate("ALTER TABLE submissions ADD COLUMN file_type VARCHAR(10)");
            } catch (Exception e) {
            } // Ignore if exists
            try {
                stmt.executeUpdate("ALTER TABLE submissions ADD COLUMN submission_data LONGBLOB");
            } catch (Exception e) {
            } // Ignore if exists

            System.out.println("MIGRATION: Checked/Created submissions table.");

            // Migration: Add department and academic_year to courses
            try {
                stmt.executeUpdate("ALTER TABLE courses ADD COLUMN department_id INT DEFAULT 1");
                System.out.println("MIGRATION: Added department_id to courses");
            } catch (Exception e) {
                // Column already exists
            }

            try {
                stmt.executeUpdate("ALTER TABLE courses ADD COLUMN academic_year_id INT DEFAULT 1");
                System.out.println("MIGRATION: Added academic_year_id to courses");
            } catch (Exception e) {
                // Column already exists
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        launch(args);
    }
}
