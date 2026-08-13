package com.school.sms.dao;

import com.school.sms.model.UniformItem;
import com.school.sms.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UniformDAO {

    public boolean checklistExists(int studentId, String term) {
        String sql = "SELECT COUNT(*) FROM Uniforms WHERE studentId = ? AND term = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, term);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check uniform checklist: " + e.getMessage(), e);
        }
    }

    public void insert(int studentId, String term, String itemName) {
        String sql = "INSERT INTO Uniforms (studentId, term, itemName, received) VALUES (?, ?, ?, FALSE)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, term);
            ps.setString(3, itemName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert uniform item: " + e.getMessage(), e);
        }
    }

    public List<UniformItem> findByStudentAndTerm(int studentId, String term) {
        List<UniformItem> results = new ArrayList<>();
        String sql = "SELECT * FROM Uniforms WHERE studentId = ? AND term = ? ORDER BY id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, term);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch uniform checklist: " + e.getMessage(), e);
        }
        return results;
    }

    /** Toggles received status and stamps the exact date/time it was checked off, or clears it if un-checking. */
    public void setReceived(int uniformItemId, boolean received, int recordedByUserId) {
        String sql = "UPDATE Uniforms SET received = ?, dateReceived = ?, recordedByUserId = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, received);
            ps.setTimestamp(2, received ? Timestamp.valueOf(LocalDateTime.now()) : null);
            ps.setInt(3, recordedByUserId);
            ps.setInt(4, uniformItemId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update uniform item: " + e.getMessage(), e);
        }
    }

    private UniformItem map(ResultSet rs) throws SQLException {
        UniformItem u = new UniformItem();
        u.setId(rs.getInt("id"));
        u.setStudentId(rs.getInt("studentId"));
        u.setTerm(rs.getString("term"));
        u.setItemName(rs.getString("itemName"));
        u.setReceived(rs.getBoolean("received"));
        Timestamp ts = rs.getTimestamp("dateReceived");
        u.setDateReceived(ts != null ? ts.toLocalDateTime() : null);
        u.setRecordedByUserId(rs.getInt("recordedByUserId"));
        return u;
    }
}
