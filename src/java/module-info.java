module com.lawyercompany {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.desktop;
    requires java.sql;
    requires com.zaxxer.hikari;
    requires org.postgresql.jdbc;
    requires jbcrypt;
    requires jdk.httpserver;
    requires java.net.http;

    opens com.lawyercompany.controller to javafx.fxml;
    opens com.lawyercompany.entity to javafx.base;
    exports com.lawyercompany;
}
