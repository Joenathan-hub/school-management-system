package com.school.sms.dao;

import com.school.sms.model.Requirement;
import com.school.sms.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RequirementDAO {

    public boolean checklistExists(int studentId, String term) {
        String sql = "SELECT COUNT(*) FROM Requirements WHERE studentId = ? AND term = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, term);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check requirements checklist: " + e.getMessage(), e);
        }
    }

    public void insert(int studentId, String term, String itemName) {
        String sql = "INSERT INTO Requirements (studentId, term, itemName, brought) VALUES (?, ?, ?, FALSE)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, term);
            ps.setString(3, itemName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert requirement: " + e.getMessage(), e);
        }
    }

    public List<Requirement> findByStudentAndTerm(int studentId, String term) {
        List<Requirement> results = new ArrayList<>();
        String sql = "SELECT * FROM Requirements WHERE studentId = ? AND term = ? ORDER BY id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, term);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch requirements checklist: " + e.getMessage(), e);
        }
        return results;
    }

    public void setBrought(int requirementId, boolean brought) {
        String sql = "UPDATE Requirements SET brought = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, brought);
            ps.setInt(2, requirementId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update requirement: " + e.getMessage(), e);
        }
    }

    private Requirement map(ResultSet rs) throws SQLException {
        Requirement r = new Requirement();
        r.setId(rs.getInt("id"));
        r.setStudentId(rs.getInt("studentId"));
        r.setTerm(rs.getString("term"));
        r.setItemName(rs.getString("itemName"));
        r.setBrought(rs.getBoolean("brought"));
        return r;
    }
}
