package com.school.sms.dao;

import com.school.sms.model.WorkerPayment;
import com.school.sms.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WorkerPaymentDAO {

    public WorkerPayment insert(int workerId, double amount, String forMonth, int recordedByUserId,
                                 String notes, LocalDateTime transactionDate) {
        String sql = """
            INSERT INTO WorkerPayments (workerId, amount, paymentDate, recordedAt, forMonth, recordedByUserId, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime txDate = transactionDate != null ? transactionDate : now;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, workerId);
            ps.setDouble(2, amount);
            ps.setTimestamp(3, Timestamp.valueOf(txDate));
            ps.setTimestamp(4, Timestamp.valueOf(now));
            ps.setString(5, forMonth);
            ps.setInt(6, recordedByUserId);
            ps.setString(7, notes);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    WorkerPayment payment = new WorkerPayment();
                    payment.setId(keys.getInt(1));
                    payment.setWorkerId(workerId);
                    payment.setAmount(amount);
                    payment.setForMonth(forMonth);
                    payment.setPaymentDateTime(txDate);
                    payment.setRecordedAt(now);
                    payment.setRecordedByUserId(recordedByUserId);
                    payment.setNotes(notes);
                    return payment;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert worker payment: " + e.getMessage(), e);
        }
        return null;
    }

    public void updateTransactionDate(int paymentId, LocalDateTime newDate) {
        String sql = "UPDATE WorkerPayments SET paymentDate = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(newDate));
            ps.setInt(2, paymentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update worker payment date: " + e.getMessage(), e);
        }
    }

    public List<WorkerPayment> findAll() {
        List<WorkerPayment> results = new ArrayList<>();
        String sql = "SELECT * FROM WorkerPayments ORDER BY paymentDate DESC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) results.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch worker payments: " + e.getMessage(), e);
        }
        return results;
    }

    public double totalPaidForMonth(int workerId, String forMonth) {
        String sql = "SELECT SUM(amount) AS total FROM WorkerPayments WHERE workerId = ? AND forMonth = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, workerId);
            ps.setString(2, forMonth);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to sum worker payments: " + e.getMessage(), e);
        }
        return 0.0;
    }

    private WorkerPayment map(ResultSet rs) throws SQLException {
        WorkerPayment w = new WorkerPayment();
        w.setId(rs.getInt("id"));
        w.setWorkerId(rs.getInt("workerId"));
        w.setAmount(rs.getDouble("amount"));
        w.setForMonth(rs.getString("forMonth"));
        Timestamp ts = rs.getTimestamp("paymentDate");
        w.setPaymentDateTime(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
        Timestamp rec = rs.getTimestamp("recordedAt");
        w.setRecordedAt(rec != null ? rec.toLocalDateTime() : null);
        w.setRecordedByUserId(rs.getInt("recordedByUserId"));
        w.setNotes(rs.getString("notes"));
        return w;
    }
}