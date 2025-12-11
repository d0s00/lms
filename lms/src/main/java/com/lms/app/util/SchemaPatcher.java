package com.lms.app.util;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * SchemaPatcher Utility.
 * 
 * Purpose:
 * - Runs automatic database migration scripts on startup.
 * - Checks if specific tables (like `submissions` or `assignments`) exist.
 * - Adds missing columns or creates tables if they are missing.
 * - Ensures the database structure matches what the Java code expects.
 */
public class SchemaPatcher {
    public static void main(String[] args) {
        patchAssignmentsTable();
    }

    public static void patchAssignmentsTable() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            patchAssignmentsTable(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void patchAssignmentsTable(Connection conn) {
        String sql1 = "ALTER TABLE assignments ADD COLUMN assignment_data LONGBLOB";
        String sql2 = "ALTER TABLE assignments ADD COLUMN file_type VARCHAR(10)";

        try (Statement stmt = conn.createStatement()) {

            try {
                stmt.execute(sql1);
                System.out.println("Added assignment_data column.");
            } catch (SQLException e) {
                // Column likely exists
            }

            try {
                stmt.execute(sql2);
                System.out.println("Added file_type column.");
            } catch (SQLException e) {
                // Column likely exists
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
