package com.voting.util;

import com.voting.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseUtil {
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private DatabaseUtil() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                AppConfig.get("mysql.url", "MYSQL_URL"),
                AppConfig.get("mysql.username", "MYSQL_USERNAME"),
                AppConfig.get("mysql.password", "MYSQL_PASSWORD")
        );
    }
}
