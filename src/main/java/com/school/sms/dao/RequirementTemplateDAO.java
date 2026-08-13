package com.school.sms.dao;

import com.school.sms.model.RequirementTemplate;
import com.school.sms.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RequirementTemplateDAO {

    public List<RequirementTemplate> findByCategory(String category) {
        List<RequirementTemplate> results = new ArrayList<>();
        String sql = "SELECT * FROM RequirementTemplates WHERE category = ? ORDER BY itemName";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch requirement templates: " + e.getMessage(), e);
        }
        return results;
    }

    public void insert(String category, String itemName) {
        String sql = "INSERT INTO RequirementTemplates (category, itemName) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);
            ps.setString(2, itemName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add requirement template: " + e.getMessage(), e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM RequirementTemplates WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove requirement template: " + e.getMessage(), e);
        }
    }

    private RequirementTemplate map(ResultSet rs) throws SQLException {
        RequirementTemplate t = new RequirementTemplate();
        t.setId(rs.getInt("id"));
        t.setCategory(rs.getString("category"));
        t.setItemName(rs.getString("itemName"));
        return t;
    }
}