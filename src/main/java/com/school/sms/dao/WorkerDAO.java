package com.school.sms.dao;

import com.school.sms.model.Worker;
import com.school.sms.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WorkerDAO {

    public int insert(Worker w) {
        String sql = """
            INSERT INTO Workers (workerId, fullName, jobTitle, contact, dateJoined, monthlySalary, active)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, w.getWorkerId());
            ps.setString(2, w.getFullName());
            ps.setString(3, w.getJobTitle());
            ps.setString(4, w.getContact());
            ps.setDate(5, Date.valueOf(w.getDateJoined()));
            ps.setDouble(6, w.getMonthlySalary());
            ps.setBoolean(7, w.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert worker: " + e.getMessage(), e);
        }
        return -1;
    }

    public boolean workerIdExists(String workerId) {
        String sql = "SELECT COUNT(*) FROM Workers WHERE workerId = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, workerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check worker ID: " + e.getMessage(), e);
        }
    }

    /** Soft delete — admin removes a worker without destroying payment history. */
    public void deactivate(int workerDbId) {
        String sql = "UPDATE Workers SET active = FALSE WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, workerDbId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to deactivate worker: " + e.getMessage(), e);
        }
    }

    public List<Worker> search(String query) {
        List<Worker> results = new ArrayList<>();
        String sql = "SELECT * FROM Workers WHERE (fullName LIKE ? OR workerId LIKE ?) AND active = TRUE";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String pattern = "%" + query + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search workers: " + e.getMessage(), e);
        }
        return results;
    }

    public List<Worker> findAll() {
        List<Worker> results = new ArrayList<>();
        String sql = "SELECT * FROM Workers WHERE active = TRUE ORDER BY fullName";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) results.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch workers: " + e.getMessage(), e);
        }
        return results;
    }

    /** Corrects a worker's actual start date without changing payroll data. */
    public void updateDateJoined(int workerId, LocalDate dateJoined) {
        String sql = "UPDATE Workers SET dateJoined = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(dateJoined));
            ps.setInt(2, workerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update worker start date: " + e.getMessage(), e);
        }
    }

    private Worker map(ResultSet rs) throws SQLException {
        Worker w = new Worker();
        w.setId(rs.getInt("id"));
        w.setWorkerId(rs.getString("workerId"));
        w.setFullName(rs.getString("fullName"));
        w.setJobTitle(rs.getString("jobTitle"));
        w.setContact(rs.getString("contact"));
        Date d = rs.getDate("dateJoined");
        w.setDateJoined(d != null ? d.toLocalDate() : LocalDate.now());
        w.setMonthlySalary(rs.getDouble("monthlySalary"));
        w.setActive(rs.getBoolean("active"));
        return w;
    }

    public Worker findById(int id) {
        String sql = "SELECT * FROM Workers WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch worker: " + e.getMessage(), e);
        }
        return null;
    }
}
