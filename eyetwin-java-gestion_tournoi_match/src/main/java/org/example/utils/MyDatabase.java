package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private static final String URL = "jdbc:mysql://localhost:3306/eyetwin_platform";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static MyDatabase instance;
    private Connection connection;

    private MyDatabase() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connection established successfully!");
        } catch (SQLException e) {
            System.err.println("Error establishing database connection: " + e.getMessage());
        }
    }

    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        } else {
            try {
                if (instance.getConnection().isClosed()) {
                    instance = new MyDatabase();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}
