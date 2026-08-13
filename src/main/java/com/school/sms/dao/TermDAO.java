package com.school.sms.dao;

import com.school.sms.model.Term;
import com.school.sms.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TermDAO {

    public Term findActive() {
        String sql = "SELECT * FROM Terms WHERE status = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "ACTIVE");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch active term: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Term> findAll() {
        List<Term> results = new ArrayList<>();
        String sql = "SELECT * FROM Terms ORDER BY startDate DESC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) results.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch terms: " + e.getMessage(), e);
        }
        return results;
    }

    public int insert(Term t) {
        String sql = "INSERT INTO Terms (name, status, startDate, endDate) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getName());
            ps.setString(2, t.getStatus());
            ps.setTimestamp(3, t.getStartDate() != null ? Timestamp.valueOf(t.getStartDate()) : null);
            ps.setTimestamp(4, t.getEndDate() != null ? Timestamp.valueOf(t.getEndDate()) : null);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert term: " + e.getMessage(), e);
        }
        return -1;
    }

    public void close(int termId) {
        String sql = "UPDATE Terms SET status = ?, endDate = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "CLOSED");
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, termId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to close term: " + e.getMessage(), e);
        }
    }

    private Term map(ResultSet rs) throws SQLException {
        Term t = new Term();
        t.setId(rs.getInt("id"));
        t.setName(rs.getString("name"));
        t.setStatus(rs.getString("status"));
        Timestamp start = rs.getTimestamp("startDate");
        t.setStartDate(start != null ? start.toLocalDateTime() : null);
        Timestamp end = rs.getTimestamp("endDate");
        t.setEndDate(end != null ? end.toLocalDateTime() : null);
        return t;
    }
}