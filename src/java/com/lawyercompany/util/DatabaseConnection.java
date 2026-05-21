package com.lawyercompany.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private final HikariDataSource dataSource;

    private DatabaseConnection() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            Properties props = new Properties();
            props.load(input);
            String jdbcUrl = props.getProperty("db.url");
            if (jdbcUrl == null || jdbcUrl.isBlank()) {
                throw new IllegalStateException("Не задан параметр db.url в config.properties");
            }

            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(jdbcUrl.trim());
            cfg.setPoolName("LawyerCompanyHikariPool");
            cfg.setMaximumPoolSize(10);
            cfg.setMinimumIdle(2);
            cfg.setConnectionTimeout(10_000);
            cfg.setValidationTimeout(5_000);
            cfg.setIdleTimeout(60_000);
            cfg.setMaxLifetime(30L * 60_000);

            this.dataSource = new HikariDataSource(cfg);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось загрузить config.properties", e);
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        dataSource.close();
    }
}