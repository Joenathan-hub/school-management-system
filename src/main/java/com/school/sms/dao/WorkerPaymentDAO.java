package com.school.sms.dao;

import com.school.sms.model.WorkerPayment;
import com.school.sms.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;

public class WorkerPaymentDAO {

    /** Records the payment with the exact date and time (24-hour clock) and returns the saved record. */
    public WorkerPayment insert(int workerId, double amount, String forMonth, int recordedByUserId, String notes) {
        String sql = """
            INSERT INTO WorkerPayments (workerId, amount, paymentDate, forMonth, recordedByUserId, notes)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        LocalDateTime now = LocalDateTime.now();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, workerId);
            ps.setDouble(2, amount);
            ps.setTimestamp(3, Timestamp.valueOf(now));
            ps.setString(4, forMonth);
            ps.setInt(5, recordedByUserId);
            ps.setString(6, notes);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    WorkerPayment payment = new WorkerPayment();
                    payment.setId(keys.getInt(1));
                    payment.setWorkerId(workerId);
                    payment.setAmount(amount);
                    payment.setForMonth(forMonth);
                    payment.setPaymentDateTime(now);
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

    /** Total paid to this worker for a specific month, used to compute their balance. */
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
}
