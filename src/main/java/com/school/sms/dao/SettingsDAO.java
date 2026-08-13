package com.school.sms.dao;

import com.school.sms.util.DatabaseManager;

import java.sql.*;

public class SettingsDAO {

    public String get(String key) {
        String sql = "SELECT settingValue FROM Settings WHERE settingKey = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("settingValue");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read setting '" + key + "': " + e.getMessage(), e);
        }
        return null;
    }

    public String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    /** Upsert: updates the setting if it exists, inserts it otherwise. */
    public void set(String key, String value) {
        String updateSql = "UPDATE Settings SET settingValue = ? WHERE settingKey = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, value);
            ps.setString(2, key);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                String insertSql = "INSERT INTO Settings (settingKey, settingValue) VALUES (?, ?)";
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    insertPs.setString(1, key);
                    insertPs.setString(2, value);
                    insertPs.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save setting '" + key + "': " + e.getMessage(), e);
        }
    }
}
