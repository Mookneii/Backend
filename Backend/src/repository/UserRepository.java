package repository;

import model.User;
import util.DBConnection;

import java.sql.*;

public class UserRepository {

    public UserRepository() {
        try {
            ensureTables();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void ensureTables() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                role TEXT NOT NULL
            )
        """;

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement()) {
            st.execute(sql);
        }
    }

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    public void createUser(String username, String passwordHash, String role) throws SQLException {
        String sql = "INSERT INTO users(username, password, role) VALUES(?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, role);
            ps.executeUpdate();
        }
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;

            return new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("role")
            );
        }
    }

    public boolean hasAnyAdmin() throws SQLException {
        String sql = "SELECT 1 FROM users WHERE role = 'ADMIN' LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next();
        }
    }
}
