package service;

import model.User;
import repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {

    private final UserRepository userRepo = new UserRepository();

    // token -> User
    private final Map<String, User> sessions = new ConcurrentHashMap<>();

    public AuthService() {
        try {
            ensureDefaultAdmin();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void ensureDefaultAdmin() throws SQLException {
        if (!userRepo.hasAnyAdmin()) {
            // default admin account
            userRepo.createUser("admin", sha256("admin123"), "ADMIN");
            System.out.println("✅ Default admin created: admin / admin123");
        }
    }

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    public String registerUser(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username and password are required");
        }

        if (userRepo.usernameExists(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        userRepo.createUser(username, sha256(password), "USER");
        return "OK";
    }

    public LoginResult login(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username and password are required");
        }

        User u = userRepo.findByUsername(username);
        if (u == null) throw new IllegalArgumentException("Invalid username or password");

        String inputHash = sha256(password);
        if (!inputHash.equals(u.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = UUID.randomUUID().toString();
        sessions.put(token, u);

        return new LoginResult(token, u.getUsername(), u.getRole());
    }

    public void logout(String token) {
        if (token != null) sessions.remove(token);
    }

    public User getUserByToken(String token) {
        if (token == null) return null;
        return sessions.get(token);
    }

    public record LoginResult(String token, String username, String role) {}
}
