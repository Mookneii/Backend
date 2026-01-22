package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.File;

public class DBConnection {

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC"); 
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC Driver not found", e);
        }

        File dbFile = new File("Backend/db/clothes_shop.db");
        System.out.println("DB Absolute Path: " + dbFile.getAbsolutePath());

        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        return DriverManager.getConnection(url);
    }
}
