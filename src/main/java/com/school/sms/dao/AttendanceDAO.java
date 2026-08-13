package com.school.sms.dao;

import com.school.sms.model.AttendanceRecord;
import com.school.sms.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDAO {

    public AttendanceRecord findOne(int studentId, String term) {
        String sql = "SELECT * FROM Attendance WHERE studentId = ? AND term = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, term);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch attendance: " + e.getMessage(), e);
        }
        return null;
    }

    public void upsert(AttendanceRecord a) {
        AttendanceRecord existing = findOne(a.getStudentId(), a.getTerm());
        if (existing == null) {
            String sql = "INSERT INTO Attendance (studentId, term, returned, recordedByUserId) VALUES (?, ?, ?, ?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, a.getStudentId());
                ps.setString(2, a.getTerm());
                ps.setBoolean(3, a.isReturned());
                ps.setInt(4, a.getRecordedByUserId());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to insert attendance: " + e.getMessage(), e);
            }
        } else {
            String sql = "UPDATE Attendance SET returned = ?, recordedByUserId = ? WHERE id = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBoolean(1, a.isReturned());
                ps.setInt(2, a.getRecordedByUserId());
                ps.setInt(3, existing.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update attendance: " + e.getMessage(), e);
            }
        }
    }

    /** Counts of returned vs not-returned for a class+term, computed against the expected roll (all active students in that class). */
    public List<AttendanceRecord> findByTerm(String term) {
        List<AttendanceRecord> results = new ArrayList<>();
        String sql = "SELECT * FROM Attendance WHERE term = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, term);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch attendance: " + e.getMessage(), e);
        }
        return results;
    }

    private AttendanceRecord map(ResultSet rs) throws SQLException {
        AttendanceRecord a = new AttendanceRecord();
        a.setId(rs.getInt("id"));
        a.setStudentId(rs.getInt("studentId"));
        a.setTerm(rs.getString("term"));
        a.setReturned(rs.getBoolean("returned"));
        a.setRecordedByUserId(rs.getInt("recordedByUserId"));
        return a;
    }
}
