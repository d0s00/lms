package com.lms.app.controller;

import com.lms.app.util.AlertHelper;
import com.lms.app.util.DatabaseConnection;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

/**
 * AdminSettings Controller.
 * 
 * Purpose:
 * - (Admin Only) System configuration interface.
 * - Manages Department creation and updates (`departments` table).
 * - Manages Academic Year scheduling (`academic_years` table).
 * - Provides tables to view and edit these settings.
 */
public class AdminSettingsController {

    @FXML
    private TextField deptNameField;
    @FXML
    private TextField deptDescField;
    @FXML
    private TableView<Department> departmentsTable;

    @FXML
    private TextField yearNameField;
    @FXML
    private CheckBox yearActiveCheck;
    @FXML
    private TableView<AcademicYear> yearsTable;

    private ObservableList<Department> departmentsList = FXCollections.observableArrayList();
    private ObservableList<AcademicYear> yearsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupDepartmentsTable();
        setupYearsTable();
        loadDepartments();
        loadAcademicYears();
    }

    private void setupDepartmentsTable() {
        TableColumn<Department, Integer> idCol = (TableColumn<Department, Integer>) departmentsTable.getColumns()
                .get(0);
        TableColumn<Department, String> nameCol = (TableColumn<Department, String>) departmentsTable.getColumns()
                .get(1);
        TableColumn<Department, String> descCol = (TableColumn<Department, String>) departmentsTable.getColumns()
                .get(2);
        TableColumn<Department, Void> actionsCol = (TableColumn<Department, Void>) departmentsTable.getColumns().get(3);

        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("Delete");

            {
                deleteBtn.getStyleClass().add("button-danger");
                deleteBtn.setOnAction(event -> {
                    Department dept = getTableView().getItems().get(getIndex());
                    deleteDepartment(dept.getId());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Department dept = getTableView().getItems().get(getIndex());
                    // Don't allow deleting "General"
                    if (dept.getId() == 1) {
                        setGraphic(null);
                    } else {
                        setGraphic(deleteBtn);
                    }
                }
            }
        });

        departmentsTable.setItems(departmentsList);
    }

    private void setupYearsTable() {
        TableColumn<AcademicYear, Integer> idCol = (TableColumn<AcademicYear, Integer>) yearsTable.getColumns().get(0);
        TableColumn<AcademicYear, String> yearCol = (TableColumn<AcademicYear, String>) yearsTable.getColumns().get(1);
        TableColumn<AcademicYear, String> statusCol = (TableColumn<AcademicYear, String>) yearsTable.getColumns()
                .get(2);
        TableColumn<AcademicYear, Void> actionsCol = (TableColumn<AcademicYear, Void>) yearsTable.getColumns().get(3);

        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        yearCol.setCellValueFactory(new PropertyValueFactory<>("yearName"));
        statusCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().isActive() ? "Active" : "Inactive"));

        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button toggleBtn = new Button();
            private final Button deleteBtn = new Button("Delete");

            {
                toggleBtn.getStyleClass().add("button-primary");
                deleteBtn.getStyleClass().add("button-danger");

                toggleBtn.setOnAction(event -> {
                    AcademicYear year = getTableView().getItems().get(getIndex());
                    toggleYearStatus(year.getId(), !year.isActive());
                });

                deleteBtn.setOnAction(event -> {
                    AcademicYear year = getTableView().getItems().get(getIndex());
                    deleteAcademicYear(year.getId());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    AcademicYear year = getTableView().getItems().get(getIndex());
                    toggleBtn.setText(year.isActive() ? "Deactivate" : "Activate");
                    HBox buttons = new HBox(5, toggleBtn, deleteBtn);
                    setGraphic(buttons);
                }
            }
        });

        yearsTable.setItems(yearsList);
    }

    @FXML
    private void addDepartment() {
        String name = deptNameField.getText().trim();
        String description = deptDescField.getText().trim();

        if (name.isEmpty()) {
            AlertHelper.showError("Error", "Department name is required");
            return;
        }

        String sql = "INSERT INTO departments (name, description) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setString(2, description.isEmpty() ? null : description);
            stmt.executeUpdate();

            AlertHelper.showSuccess("Success", "Department added successfully");
            deptNameField.clear();
            deptDescField.clear();
            loadDepartments();

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate")) {
                AlertHelper.showError("Error", "Department already exists");
            } else {
                e.printStackTrace();
                AlertHelper.showError("Error", "Failed to add department");
            }
        }
    }

    private void deleteDepartment(int id) {
        if (id == 1) {
            AlertHelper.showError("Error", "Cannot delete General department");
            return;
        }

        String sql = "DELETE FROM departments WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

            AlertHelper.showSuccess("Success", "Department deleted");
            loadDepartments();

        } catch (SQLException e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "Failed to delete department");
        }
    }

    @FXML
    private void addAcademicYear() {
        String yearName = yearNameField.getText().trim();
        boolean isActive = yearActiveCheck.isSelected();

        if (yearName.isEmpty()) {
            AlertHelper.showError("Error", "Year name is required");
            return;
        }

        String sql = "INSERT INTO academic_years (year_name, is_active) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, yearName);
            stmt.setBoolean(2, isActive);
            stmt.executeUpdate();

            AlertHelper.showSuccess("Success", "Academic year added");
            yearNameField.clear();
            yearActiveCheck.setSelected(true);
            loadAcademicYears();

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate")) {
                AlertHelper.showError("Error", "Academic year already exists");
            } else {
                e.printStackTrace();
                AlertHelper.showError("Error", "Failed to add academic year");
            }
        }
    }

    private void toggleYearStatus(int id, boolean newStatus) {
        String sql = "UPDATE academic_years SET is_active = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, newStatus);
            stmt.setInt(2, id);
            stmt.executeUpdate();

            loadAcademicYears();

        } catch (SQLException e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "Failed to update status");
        }
    }

    private void deleteAcademicYear(int id) {
        String sql = "DELETE FROM academic_years WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

            AlertHelper.showSuccess("Success", "Academic year deleted");
            loadAcademicYears();

        } catch (SQLException e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "Failed to delete academic year");
        }
    }

    private void loadDepartments() {
        departmentsList.clear();
        String sql = "SELECT * FROM departments ORDER BY id";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                departmentsList.add(new Department(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadAcademicYears() {
        yearsList.clear();
        String sql = "SELECT * FROM academic_years ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                yearsList.add(new AcademicYear(
                        rs.getInt("id"),
                        rs.getString("year_name"),
                        rs.getBoolean("is_active")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Inner classes for data models
    public static class Department {
        private final int id;
        private final String name;
        private final String description;

        public Department(int id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class AcademicYear {
        private final int id;
        private final String yearName;
        private final boolean active;

        public AcademicYear(int id, String yearName, boolean active) {
            this.id = id;
            this.yearName = yearName;
            this.active = active;
        }

        public int getId() {
            return id;
        }

        public String getYearName() {
            return yearName;
        }

        public boolean isActive() {
            return active;
        }
    }
}
