package com.lms.app.controller;

import com.lms.app.util.UserSession;
import com.lms.app.util.DatabaseConnection;
import com.lms.app.util.FileHandler;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.shape.Circle;
import javafx.geometry.Pos;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Dashboard Controller.
 * 
 * Purpose:
 * - The main container implementation for the application.
 * - Manages the Sidebar navigation menu, showing/hiding buttons based on User
 * Role.
 * - Handles the center pane switching (loading different Views like
 * CourseManager, Profile).
 * - Manages Logout logic.
 */
public class DashboardController {

    @FXML
    private BorderPane mainLayout;

    @FXML
    private VBox sidebar;

    @FXML
    private Label windowTitle;

    /**
     * Initializes the Dashboard Logic.
     * 
     * Logic Flow:
     * 1. Called automatically by JavaFX after the FXML is loaded.
     * 2. Retrieves the `UserSession` to identify the current user.
     * 3. Updates the `usernameLabel` to welcome the user.
     * 4. Hides ALL sidebar buttons by default (security by default).
     * 5. Checks the User Role (Student/Instructor/Admin) and un-hides only the
     * relevant buttons.
     * - Example: Students only see "Catalog" and "My Grades".
     * 6. Loads the default view (e.g., Profile or Catalog) into the center pane.
     */
    @FXML
    public void initialize() {
        String role = UserSession.getInstance().getRole();
        int userId = UserSession.getInstance().getUserId();

        // Add user profile section at the top
        addUserProfileSection(userId);

        if ("Admin".equalsIgnoreCase(role)) {
            Button manageUsersBtn = createNavButton("Manage Users");
            manageUsersBtn.setOnAction(e -> loadView("UserManagerView.fxml"));

            Button settingsBtn = createNavButton("Settings");
            settingsBtn.setOnAction(e -> loadView("AdminSettingsView.fxml"));

            sidebar.getChildren().addAll(manageUsersBtn, settingsBtn);
        }

        if ("Instructor".equalsIgnoreCase(role)) {
            Button manageCoursesBtn = createNavButton("Manage Courses");
            manageCoursesBtn.setOnAction(e -> loadView("CourseManager.fxml"));

            Button gradingBtn = createNavButton("Grading");
            gradingBtn.setOnAction(e -> loadView("GradingView.fxml"));

            sidebar.getChildren().addAll(manageCoursesBtn, gradingBtn);
        } else if ("Student".equalsIgnoreCase(role)) {
            Button catalogBtn = createNavButton("Course Catalog");
            catalogBtn.setOnAction(e -> loadView("StudentCatalog.fxml"));

            Button gradesBtn = createNavButton("My Grades");
            gradesBtn.setOnAction(e -> loadView("StudentGradesView.fxml"));

            sidebar.getChildren().addAll(catalogBtn, gradesBtn);
        }

        Button profileBtn = createNavButton("My Profile");
        profileBtn.setOnAction(e -> loadProfileView());

        sidebar.getChildren().add(profileBtn);

        // Add spacer to push logout button to bottom
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Add logout button at the bottom
        Button logoutBtn = new Button("Logout");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.getStyleClass().add("button-danger");
        logoutBtn.setOnAction(e -> handleLogout());

        sidebar.getChildren().addAll(spacer, logoutBtn);
    }

    private void addUserProfileSection(int userId) {
        try {
            String sql = "SELECT username, profile_image FROM users WHERE id = ?";
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                VBox profileBox = new VBox(10);
                profileBox.setAlignment(Pos.CENTER);
                profileBox.setStyle("-fx-padding: 20 10 20 10; -fx-background-color: rgba(0,0,0,0.2);");

                // User image
                ImageView userImageView = new ImageView();
                userImageView.setFitWidth(80);
                userImageView.setFitHeight(80);
                userImageView.setPreserveRatio(false);

                // Make it circular
                Circle clip = new Circle(40, 40, 40);
                userImageView.setClip(clip);

                byte[] imageData = rs.getBytes("profile_image");
                if (imageData != null && imageData.length > 0) {
                    try {
                        userImageView.setImage(FileHandler.getImageFromBytes(imageData));
                    } catch (Exception e) {
                        // Use default placeholder
                        userImageView.setImage(getDefaultUserImage());
                    }
                } else {
                    userImageView.setImage(getDefaultUserImage());
                }

                // Username label
                Label usernameLabel = new Label(rs.getString("username"));
                usernameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

                profileBox.getChildren().addAll(userImageView, usernameLabel);
                sidebar.getChildren().add(0, profileBox);
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.err.println("Error loading user profile: " + e.getMessage());
        }
    }

    private Image getDefaultUserImage() {
        // Create a simple colored circle as default
        Canvas canvas = new Canvas(80, 80);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#007bff"));
        gc.fillOval(0, 0, 80, 80);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        gc.fillText("?", 30, 55);

        WritableImage image = new WritableImage(80, 80);
        canvas.snapshot(null, image);
        return image;
    }

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 12 20; " +
                        "-fx-alignment: CENTER_LEFT; " +
                        "-fx-border-width: 0 0 0 3; " +
                        "-fx-border-color: transparent; " +
                        "-fx-background-radius: 0; " +
                        "-fx-cursor: hand;");

        // Hover effect
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 12 20; " +
                        "-fx-alignment: CENTER_LEFT; " +
                        "-fx-border-width: 0 0 0 3; " +
                        "-fx-border-color: #3498db; " +
                        "-fx-background-radius: 0; " +
                        "-fx-cursor: hand;"));

        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 12 20; " +
                        "-fx-alignment: CENTER_LEFT; " +
                        "-fx-border-width: 0 0 0 3; " +
                        "-fx-border-color: transparent; " +
                        "-fx-background-radius: 0; " +
                        "-fx-cursor: hand;"));

        return btn;
    }

    private void loadProfileView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/app/UserProfileView.fxml"));
            Parent view = loader.load();

            UserProfileController controller = loader.getController();
            controller.setTargetUserId(UserSession.getInstance().getUserId());

            mainLayout.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadView(String fxmlFile) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/com/lms/app/" + fxmlFile));
            mainLayout.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleLogout() {
        UserSession.cleanSession();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/lms/app/LoginView.fxml"));
            mainLayout.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
