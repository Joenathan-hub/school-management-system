package com.school.sms.dao;

import com.school.sms.model.Student;
import com.school.sms.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {

    public int insert(Student s) {
        String sql = """
            INSERT INTO Students (studentId, fullName, age, sex, studentClass, boardingStatus, admissionDate, fatherId, motherId, active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getStudentId());
            ps.setString(2, s.getFullName());
            ps.setInt(3, s.getAge());
            ps.setString(4, s.getSex());
            ps.setString(5, s.getStudentClass());
            ps.setString(6, s.getBoardingStatus());
            ps.setDate(7, Date.valueOf(s.getAdmissionDate()));
            ps.setInt(8, s.getFatherId());
            ps.setInt(9, s.getMotherId());
            ps.setBoolean(10, s.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert student: " + e.getMessage(), e);
        }
        return -1;
    }

    public boolean studentIdExists(String studentId) {
        String sql = "SELECT COUNT(*) FROM Students WHERE studentId = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check student ID: " + e.getMessage(), e);
        }
    }

    public Student findByStudentId(String studentId) {
        String sql = "SELECT * FROM Students WHERE studentId = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch student: " + e.getMessage(), e);
        }
        return null;
    }

    public Student findById(int id) {
        String sql = "SELECT * FROM Students WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch student: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Student> search(String query) {
        List<Student> results = new ArrayList<>();
        String sql = "SELECT * FROM Students WHERE fullName LIKE ? OR studentId LIKE ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String pattern = "%" + query + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search students: " + e.getMessage(), e);
        }
        return results;
    }

    public List<Student> findAll() {
        List<Student> results = new ArrayList<>();
        String sql = "SELECT * FROM Students WHERE active = TRUE ORDER BY studentClass, fullName";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) results.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all students: " + e.getMessage(), e);
        }
        return results;
    }

    public List<Student> findByClass(String studentClass) {
        List<Student> results = new ArrayList<>();
        String sql = "SELECT * FROM Students WHERE studentClass = ? AND active = TRUE";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentClass);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch class list: " + e.getMessage(), e);
        }
        return results;
    }

    /** Corrects the date a student reported without changing other admission data. */
    public void updateAdmissionDate(int studentId, LocalDate admissionDate) {
        String sql = "UPDATE Students SET admissionDate = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(admissionDate));
            ps.setInt(2, studentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update student reporting date: " + e.getMessage(), e);
        }
    }

    private Student map(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getInt("id"));
        s.setStudentId(rs.getString("studentId"));
        s.setFullName(rs.getString("fullName"));
        s.setAge(rs.getInt("age"));
        s.setSex(rs.getString("sex"));
        s.setStudentClass(rs.getString("studentClass"));
        s.setBoardingStatus(rs.getString("boardingStatus"));
        Date admissionDate = rs.getDate("admissionDate");
        s.setAdmissionDate(admissionDate != null ? admissionDate.toLocalDate() : LocalDate.now());
        s.setFatherId(rs.getInt("fatherId"));
        s.setMotherId(rs.getInt("motherId"));
        s.setActive(rs.getBoolean("active"));
        return s;
    }
}
