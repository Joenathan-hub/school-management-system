package com.school.sms.dao;

import com.school.sms.model.SchoolEvent;
import com.school.sms.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SchoolEventDAO {

    public int insert(SchoolEvent event) {
        String sql = "INSERT INTO SchoolEvents (title, eventDate, description, createdByUserId) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, event.getTitle());
            ps.setDate(2, event.getEventDate() != null ? Date.valueOf(event.getEventDate()) : null);
            ps.setString(3, event.getDescription());
            ps.setInt(4, event.getCreatedByUserId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert event: " + e.getMessage(), e);
        }
        return -1;
    }

    public List<SchoolEvent> findAll() {
        List<SchoolEvent> results = new ArrayList<>();
        String sql = "SELECT * FROM SchoolEvents ORDER BY eventDate";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) results.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch events: " + e.getMessage(), e);
        }
        return results;
    }

    public List<SchoolEvent> findUpcoming(int limit) {
        LocalDate today = LocalDate.now();
        return findAll().stream()
                .filter(ev -> ev.getEventDate() != null && !ev.getEventDate().isBefore(today))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public void delete(int id) {
        String sql = "DELETE FROM SchoolEvents WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete event: " + e.getMessage(), e);
        }
    }

    private SchoolEvent map(ResultSet rs) throws SQLException {
        SchoolEvent e = new SchoolEvent();
        e.setId(rs.getInt("id"));
        e.setTitle(rs.getString("title"));
        Date d = rs.getDate("eventDate");
        e.setEventDate(d != null ? d.toLocalDate() : null);
        e.setDescription(rs.getString("description"));
        e.setCreatedByUserId(rs.getInt("createdByUserId"));
        return e;
    }
}