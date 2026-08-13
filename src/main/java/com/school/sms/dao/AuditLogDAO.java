package com.school.sms.dao;

import com.school.sms.model.AuditLogEntry;
import com.school.sms.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    public void log(int userId, String action, String details) {
        String sql = "INSERT INTO AuditLog (userId, action, details, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.setString(3, details);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            // Audit logging must never crash the primary operation it's attached to.
            System.err.println("Failed to write audit log: " + e.getMessage());
        }
    }

    /** Full history, most recent first, with the acting user's name joined in (LEFT JOIN so removed users still show). */
    public List<AuditLogEntry> findAll() {
        List<AuditLogEntry> results = new ArrayList<>();
        String sql = "SELECT a.id, a.userId, u.fullName AS userFullName, a.action, a.details, a.timestamp " +
                "FROM AuditLog a LEFT JOIN Users u ON a.userId = u.id ORDER BY a.timestamp DESC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                results.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch audit log: " + e.getMessage(), e);
        }
        return results;
    }

    private AuditLogEntry map(ResultSet rs) throws SQLException {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setId(rs.getInt("id"));
        entry.setUserId(rs.getInt("userId"));
        entry.setUserFullName(rs.getString("userFullName"));
        entry.setAction(rs.getString("action"));
        entry.setDetails(rs.getString("details"));
        Timestamp ts = rs.getTimestamp("timestamp");
        entry.setTimestamp(ts != null ? ts.toLocalDateTime() : null);
        return entry;
    }
}