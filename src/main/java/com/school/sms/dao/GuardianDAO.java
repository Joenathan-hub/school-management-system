package com.school.sms.dao;

import com.school.sms.model.Guardian;
import com.school.sms.util.DatabaseManager;

import java.sql.*;

public class GuardianDAO {

    public int insert(Guardian g) {
        String sql = "INSERT INTO Guardians (fullName, relationship, occupation, contact) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, g.getFullName());
            ps.setString(2, g.getRelationship());
            ps.setString(3, g.getOccupation());
            ps.setString(4, g.getContact());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert guardian: " + e.getMessage(), e);
        }
        return -1;
    }

    public Guardian findById(int id) {
        String sql = "SELECT * FROM Guardians WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch guardian: " + e.getMessage(), e);
        }
        return null;
    }

    private Guardian map(ResultSet rs) throws SQLException {
        return new Guardian(
                rs.getInt("id"),
                rs.getString("fullName"),
                rs.getString("relationship"),
                rs.getString("occupation"),
                rs.getString("contact")
        );
    }
}
