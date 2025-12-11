module com.lms.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires java.sql;
    requires java.desktop;
    requires mysql.connector.j;

    opens com.lms.app to javafx.fxml, javafx.graphics, javafx.base;
    opens com.lms.app.controller to javafx.fxml;
    opens com.lms.app.model to javafx.base;

    exports com.lms.app;
    exports com.lms.app.controller;
    exports com.lms.app.model;
    exports com.lms.app.util;
}
