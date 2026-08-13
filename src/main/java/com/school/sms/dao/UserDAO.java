package com.school.sms.dao;

import com.school.sms.model.Role;
import com.school.sms.model.User;
import com.school.sms.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public int countUsers() {
        String sql = "SELECT COUNT(*) FROM Users";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count users: " + e.getMessage(), e);
        }
        return 0;
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM Users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch user: " + e.getMessage(), e);
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT * FROM Users WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch user: " + e.getMessage(), e);
        }
        return null;
    }

    public List<User> findAll() {
        List<User> results = new ArrayList<>();
        String sql = "SELECT * FROM Users WHERE active = TRUE ORDER BY role, fullName";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) results.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch users: " + e.getMessage(), e);
        }
        return results;
    }

    public int insert(User u) {
        String sql = "INSERT INTO Users (username, passwordHash, role, fullName, assignedClass, active) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getRole().name());
            ps.setString(4, u.getFullName());
            ps.setString(5, u.getAssignedClass());
            ps.setBoolean(6, u.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert user: " + e.getMessage(), e);
        }
        return -1;
    }

    public void updatePasswordHash(int userId, String newHash) {
        String sql = "UPDATE Users SET passwordHash = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update password: " + e.getMessage(), e);
        }
    }

    public void deactivate(int userId) {
        String sql = "UPDATE Users SET active = FALSE WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to deactivate user: " + e.getMessage(), e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("passwordHash"),
                Role.valueOf(rs.getString("role")),
                rs.getString("fullName"),
                rs.getBoolean("active")
        );
        u.setAssignedClass(rs.getString("assignedClass"));
        return u;
    }
}
