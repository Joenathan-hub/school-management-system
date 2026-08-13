package com.school.sms.dao;

import com.school.sms.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;

public class PasswordResetDAO {

    /** Stores the reset code hashed, same as a password — it's a credential too. */
    public void createCode(int userId, String codeHash) {
        String sql = "INSERT INTO PasswordResetCodes (userId, codeHash, createdAt, used) VALUES (?, ?, ?, FALSE)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, codeHash);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create reset code: " + e.getMessage(), e);
        }
    }

    public static class ResetCodeRecord {
        public int id;
        public int userId;
        public String codeHash;
    }

    /** Returns all unused reset codes for a user (there's usually just one active at a time). */
    public ResetCodeRecord findLatestUnused(int userId) {
        String sql = "SELECT TOP 1 id, userId, codeHash FROM PasswordResetCodes WHERE userId = ? AND used = FALSE ORDER BY createdAt DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ResetCodeRecord r = new ResetCodeRecord();
                    r.id = rs.getInt("id");
                    r.userId = rs.getInt("userId");
                    r.codeHash = rs.getString("codeHash");
                    return r;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reset code: " + e.getMessage(), e);
        }
        return null;
    }

    public void markUsed(int codeId) {
        String sql = "UPDATE PasswordResetCodes SET used = TRUE WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, codeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to mark reset code used: " + e.getMessage(), e);
        }
    }
}
