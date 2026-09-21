package com.school.sms.dao;

import com.school.sms.model.Expenditure;
import com.school.sms.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExpenditureDAO {

    public int insert(Expenditure e) {
        String sql = "INSERT INTO Expenditures (description, amount, category, expenditureDate, recordedAt, recordedByUserId) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getDescription());
            ps.setDouble(2, e.getAmount());
            ps.setString(3, e.getCategory());
            ps.setTimestamp(4, Timestamp.valueOf(e.getDate()));
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(6, e.getRecordedByUserId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to insert expenditure: " + ex.getMessage(), ex);
        }
        return -1;
    }

    public void updateTransactionDate(int expenditureId, LocalDateTime newDate) {
        String sql = "UPDATE Expenditures SET expenditureDate = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(newDate));
            ps.setInt(2, expenditureId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update expenditure date: " + e.getMessage(), e);
        }
    }

    public List<Expenditure> findAll() {
        List<Expenditure> results = new ArrayList<>();
        String sql = "SELECT * FROM Expenditures ORDER BY expenditureDate DESC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) results.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch expenditures: " + e.getMessage(), e);
        }
        return results;
    }

    public double totalExpenditure() {
        String sql = "SELECT SUM(amount) AS total FROM Expenditures";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble("total");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to sum expenditure: " + e.getMessage(), e);
        }
        return 0.0;
    }

    private Expenditure map(ResultSet rs) throws SQLException {
        Expenditure e = new Expenditure();
        e.setId(rs.getInt("id"));
        e.setDescription(rs.getString("description"));
        e.setAmount(rs.getDouble("amount"));
        e.setCategory(rs.getString("category"));
        Timestamp ts = rs.getTimestamp("expenditureDate");
        e.setDate(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
        Timestamp rec = rs.getTimestamp("recordedAt");
        e.setRecordedAt(rec != null ? rec.toLocalDateTime() : null);
        e.setRecordedByUserId(rs.getInt("recordedByUserId"));
        return e;
    }
}