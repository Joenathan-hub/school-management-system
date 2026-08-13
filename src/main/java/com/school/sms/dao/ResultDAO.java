package com.school.sms.dao;

import com.school.sms.model.Result;
import com.school.sms.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResultDAO {

    /** One row per student/term/subject. If it already exists, update instead of duplicating. */
    public Result findOne(int studentId, String term, String subject) {
        String sql = "SELECT * FROM Results WHERE studentId = ? AND term = ? AND subject = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, term);
            ps.setString(3, subject);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch result: " + e.getMessage(), e);
        }
        return null;
    }

    public void upsert(Result r) {
        Result existing = findOne(r.getStudentId(), r.getTerm(), r.getSubject());
        if (existing == null) {
            String sql = """
                INSERT INTO Results (studentId, term, subject, marks, grade, comment, enteredByUserId)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, r.getStudentId());
                ps.setString(2, r.getTerm());
                ps.setString(3, r.getSubject());
                ps.setInt(4, r.getMarks());
                ps.setString(5, r.getGrade());
                ps.setString(6, r.getComment());
                ps.setInt(7, r.getEnteredByUserId());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to insert result: " + e.getMessage(), e);
            }
        } else {
            String sql = "UPDATE Results SET marks = ?, grade = ?, comment = ?, enteredByUserId = ? WHERE id = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, r.getMarks());
                ps.setString(2, r.getGrade());
                ps.setString(3, r.getComment());
                ps.setInt(4, r.getEnteredByUserId());
                ps.setInt(5, existing.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update result: " + e.getMessage(), e);
            }
        }
    }

    public List<Result> findByStudentAndTerm(int studentId, String term) {
        List<Result> results = new ArrayList<>();
        String sql = "SELECT * FROM Results WHERE studentId = ? AND term = ? ORDER BY subject";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, term);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch results: " + e.getMessage(), e);
        }
        return results;
    }

    private Result map(ResultSet rs) throws SQLException {
        Result r = new Result();
        r.setId(rs.getInt("id"));
        r.setStudentId(rs.getInt("studentId"));
        r.setTerm(rs.getString("term"));
        r.setSubject(rs.getString("subject"));
        r.setMarks(rs.getInt("marks"));
        r.setGrade(rs.getString("grade"));
        r.setComment(rs.getString("comment"));
        r.setEnteredByUserId(rs.getInt("enteredByUserId"));
        return r;
    }
}
