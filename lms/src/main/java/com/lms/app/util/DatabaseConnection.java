package com.lms.app.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseConnection Utility.
 * 
 * Purpose:
 * - Manages the JDBC connection to the MySQL database.
 * - Implements the Singleton Pattern to ensure only one connection instance
 * exists.
 * - Loads database credentials (URL, User, Password) dynamically from
 * `src/main/resources/config.properties`.
 * - Provides the `getConnection()` method used by all Controllers to execute
 * SQL queries.
 */
public class DatabaseConnection {

    private static DatabaseConnection instance;
    private Connection connection;
    private static String url;
    private static String user;
    private static String password;

    private DatabaseConnection() {
        try {
            loadProperties();
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException | java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void loadProperties() throws java.io.IOException {
        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return;
            }
            props.load(input);
            url = props.getProperty("db.url");
            user = props.getProperty("db.user");
            password = props.getProperty("db.password");
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
            if (instance.getConnection() != null) {
                SchemaPatcher.patchAssignmentsTable(instance.getConnection());
            }
        } else {
            try {
                if (instance.getConnection() == null || instance.getConnection().isClosed()) {
                    instance = new DatabaseConnection();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    /**
     * Retrieves the active database connection.
     * 
     * Logic:
     * 1. Checks if the `connection` object is null or has been closed.
     * 2. If valid, returns the existing connection (Singleton behavior).
     * 3. If invalid:
     * - Loads the database URL, User, and Password logic from `config.properties`.
     * - Calls `DriverManager.getConnection()` to establish a new link to MySQL.
     * - Updates the static `connection` variable.
     * 
     * @return The active Connection object.
     * @throws SQLException If the database is unreachable or credentials are wrong.
     */
    public Connection getConnection() {
        return connection;
    }
}
