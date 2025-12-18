package com.lms.app.controller;

import com.lms.app.model.Module;
import com.lms.app.model.*;
import com.lms.app.util.FileHandler;
import com.lms.app.util.DatabaseConnection;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * ModuleEditor Controller.
 * 
 * Purpose:
 * - (Instructor Only) Pop-up dialog for managing content within a course.
 * - Allows Adding new Modules (files).
 * - Allows Creating Assignments linked to modules.
 * - Handles file uploads and input validation for new content.
 */
public class ModuleEditorController {

    private int courseId;

    @FXML
    private TableView<Module> moduleTable;
    @FXML
    private TableColumn<Module, String> modTitleCol;
    @FXML
    private TableColumn<Module, String> modTypeCol;
    @FXML
    private TableColumn<Module, LocalDate> modDateCol;
    @FXML
    private TableColumn<Module, Void> modActionCol;

    @FXML
    private TextField modTitleField;
    @FXML
    private Label statusLbl;
    @FXML
    private Button fileBtn;

    // Assignment fields
    @FXML
    private TextArea assignDescField;
    @FXML
    private TextField maxScoreField;
    @FXML
    private DatePicker dueDatePicker;
    @FXML
    private ComboBox<Module> moduleSelector;
    @FXML
    private Button assignFileBtn; // Button for assignment file
    @FXML
    private ListView<String> assignmentList;

    private File selectedDoc;
    private File selectedAssignmentDoc; // File for assignment

    public void setCourseId(int id) {
        this.courseId = id;
        loadModules();
    }

    public void initialize() {
        modTitleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        modTypeCol.setCellValueFactory(new PropertyValueFactory<>("fileType"));
        modDateCol.setCellValueFactory(new PropertyValueFactory<>("uploadDate"));

        modDateCol.setCellFactory(column -> new TableCell<Module, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                }
            }
        });

        // Delete Button Column
        modActionCol.setCellFactory(param -> new TableCell<Module, Void>() {
            private final Button deleteBtn = new Button("Delete");
            {
                deleteBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
                deleteBtn.setOnAction(event -> {
                    Module data = getTableView().getItems().get(getIndex());
                    deleteModule(data);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteBtn);
                }
            }
        });

        // Auto-sizing
        moduleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadModules() {
        ObservableList<Module> list = FXCollections.observableArrayList();
        String sql = "SELECT * FROM modules WHERE course_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, courseId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new Module(
                        rs.getInt("id"),
                        rs.getInt("course_id"),
                        rs.getString("title"),
                        rs.getBytes("module_data"),
                        rs.getString("file_type"),
                        rs.getDate("upload_date") != null ? rs.getDate("upload_date").toLocalDate() : null));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("DEBUG: Loaded " + list.size() + " modules for course " + courseId);
        moduleTable.setItems(list);
        moduleSelector.setItems(list);

        // Custom simple cell factory for combobox to show title
        moduleSelector.setConverter(new javafx.util.StringConverter<Module>() {
            @Override
            public String toString(Module object) {
                return object == null ? "" : object.getTitle();
            }

            @Override
            public Module fromString(String string) {
                return moduleTable.getItems().stream().filter(m -> m.getTitle().equals(string)).findFirst()
                        .orElse(null);
            }
        });
    }

    public void chooseFile() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Documents/Videos", "*.pdf", "*.mp4", "*.png", "*.jpg", "*.docx",
                        "*.pptx", "*.txt", "*.zip", "*.rar"));
        selectedDoc = fc.showOpenDialog(null);
        if (selectedDoc != null) {
            fileBtn.setText(selectedDoc.getName());
            if (modTitleField.getText().isEmpty()) {
                String name = selectedDoc.getName();
                int dotIndex = name.lastIndexOf(".");
                if (dotIndex > 0) {
                    name = name.substring(0, dotIndex);
                }
                modTitleField.setText(name);
            }
        }
    }

    public void addModule() {
        String title = modTitleField.getText();

        if (title.isEmpty() || selectedDoc == null) {
            statusLbl.setText("Please select a file and enter a title.");
            statusLbl.setStyle("-fx-text-fill: red;");
            return;
        }

        if (this.courseId == 0) {
            statusLbl.setText("Error: Course ID not set.");
            statusLbl.setStyle("-fx-text-fill: red;");
            return;
        }

        // Check file size (max 16MB for safety with default MySQL settings)
        if (selectedDoc.length() > 16 * 1024 * 1024) {
            statusLbl.setText("File too large (>16MB).");
            statusLbl.setStyle("-fx-text-fill: red;");
            return;
        }

        String fileName = selectedDoc.getName();
        String fileType = "file";
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
            fileType = fileName.substring(dotIndex + 1);
        }

        // Truncate to 10 chars to fit database column
        if (fileType.length() > 10) {
            fileType = fileType.substring(0, 10);
        }

        String sql = "INSERT INTO modules (course_id, title, module_data, file_type, upload_date) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, courseId);
            stmt.setString(2, title);
            stmt.setBytes(3, FileHandler.readFileToBytes(selectedDoc));
            stmt.setString(4, fileType);
            stmt.setDate(5, Date.valueOf(LocalDate.now())); // Set current date
            stmt.executeUpdate();

            loadModules();
            statusLbl.setText("Module Added Successfully!");
            statusLbl.setStyle("-fx-text-fill: green;");
            modTitleField.clear();
            selectedDoc = null;
            fileBtn.setText("Upload File");
        } catch (SQLException e) {
            e.printStackTrace();
            statusLbl.setText("DB Error: " + e.getMessage());
            statusLbl.setStyle("-fx-text-fill: red;");
        }
    }

    public void deleteModule(Module module) {
        if (module == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete module '" + module.getTitle() + "' and its assignments?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();

        if (confirm.getResult() == ButtonType.YES) {
            try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
                // First delete assignments for this module
                String delAssigns = "DELETE FROM assignments WHERE module_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(delAssigns)) {
                    stmt.setInt(1, module.getId());
                    stmt.executeUpdate();
                }

                // Then delete module
                String sql = "DELETE FROM modules WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, module.getId());
                    stmt.executeUpdate();
                    loadModules();
                    statusLbl.setText("Module Deleted.");
                    statusLbl.setStyle("-fx-text-fill: green;");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                statusLbl.setText("Deletion Failed: " + e.getMessage());
                statusLbl.setStyle("-fx-text-fill: red;");
            }
        }
    }

    public void deleteModule() {
        // Deprecated, keep for FXML compatibility if needed, but redirects to proper
        // method
        Module selected = moduleTable.getSelectionModel().getSelectedItem();
        if (selected != null)
            deleteModule(selected);
    }

    public void chooseAssignmentFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Assignment File");
        selectedAssignmentDoc = fc.showOpenDialog(null);
        if (selectedAssignmentDoc != null) {
            assignFileBtn.setText(selectedAssignmentDoc.getName());
        }
    }

    public void addAssignment() {
        Module selectedModule = moduleSelector.getValue();
        String desc = assignDescField.getText();
        String maxScoreStr = maxScoreField.getText();
        LocalDate dueDate = dueDatePicker.getValue();

        if (selectedModule == null || desc.isEmpty() || maxScoreStr.isEmpty() || dueDate == null) {
            statusLbl.setText("Please fill all assignment fields.");
            statusLbl.setStyle("-fx-text-fill: red;");
            return;
        }

        try {
            int maxScore = Integer.parseInt(maxScoreStr);
            String sql = "INSERT INTO assignments (module_id, description, max_score, due_date, assignment_data, file_type) VALUES (?, ?, ?, ?, ?, ?)";

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, selectedModule.getId());
                stmt.setString(2, desc);
                stmt.setInt(3, maxScore);
                stmt.setDate(4, Date.valueOf(dueDate));

                if (selectedAssignmentDoc != null) {
                    if (selectedAssignmentDoc.length() > 16 * 1024 * 1024) {
                        statusLbl.setText("Assignment file too large (>16MB).");
                        statusLbl.setStyle("-fx-text-fill: red;");
                        return;
                    }

                    stmt.setBytes(5, FileHandler.readFileToBytes(selectedAssignmentDoc));
                    String fileName = selectedAssignmentDoc.getName();
                    String ext = "file";
                    int dotIndex = fileName.lastIndexOf(".");
                    if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
                        ext = fileName.substring(dotIndex + 1);
                    }
                    if (ext.length() > 10)
                        ext = ext.substring(0, 10);

                    stmt.setString(6, ext);
                } else {
                    stmt.setBytes(5, null);
                    stmt.setString(6, null); // file_type can be null
                }

                stmt.executeUpdate();
                statusLbl.setText("Assignment Added!");
                statusLbl.setStyle("-fx-text-fill: green;");

                // Clear fields & Refresh List
                assignDescField.clear();
                maxScoreField.clear();
                dueDatePicker.setValue(null);
                assignFileBtn.setText("Attach File (Optional)");
                selectedAssignmentDoc = null;
                onModuleSelected(); // Refresh list
            }
        } catch (NumberFormatException e) {
            statusLbl.setText("Max Score must be a number.");
            statusLbl.setStyle("-fx-text-fill: red;");
        } catch (SQLException e) {
            e.printStackTrace();
            statusLbl.setText("DB Error: " + e.getMessage());
            statusLbl.setStyle("-fx-text-fill: red;");
        } catch (Exception e) {
            e.printStackTrace();
            statusLbl.setText("Error: " + e.getMessage());
            statusLbl.setStyle("-fx-text-fill: red;");
        }
    }

    public void onModuleSelected() {
        Module mod = moduleSelector.getValue();
        assignmentList.getItems().clear();
        if (mod == null)
            return;

        String sql = "SELECT id, description FROM assignments WHERE module_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, mod.getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                // Format: "ID: Description" for easy parsing
                assignmentList.getItems().add(rs.getInt("id") + ": " + rs.getString("description"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteAssignment() {
        String selected = assignmentList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setContentText("Select an assignment to delete.");
            a.show();
            return;
        }

        int id = Integer.parseInt(selected.split(":")[0]);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete assignment?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();
        if (confirm.getResult() == ButtonType.YES) {
            String sql = "DELETE FROM assignments WHERE id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
                statusLbl.setText("Assignment Deleted.");
                statusLbl.setStyle("-fx-text-fill: green;");
                onModuleSelected(); // refresh
            } catch (SQLException e) {
                e.printStackTrace();
                statusLbl.setText("Error deleting assignment.");
            }
        }

    }

    @FXML
    private void handleClose() {
        if (statusLbl.getScene() != null && statusLbl.getScene().getWindow() != null) {
            ((javafx.stage.Stage) statusLbl.getScene().getWindow()).close();
        }
    }
}
