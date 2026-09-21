package com.school.sms.dao;

import com.school.sms.model.Payment;
import com.school.sms.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    /** Sequential receipt numbers make audits far easier than timestamp-only records. */
    public int nextReceiptNumber() {
        String sql = "SELECT MAX(receiptNumber) AS maxReceipt FROM Payments";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                int max = rs.getInt("maxReceipt");
                return max + 1; // getInt returns 0 if NULL, so this correctly starts at 1
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to compute next receipt number: " + e.getMessage(), e);
        }
        return 1;
    }

    public int insert(Payment p) {
        String sql = """
            INSERT INTO Payments (receiptNumber, studentId, amount, term, paymentDate, recordedAt, recordedByUserId, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getReceiptNumber());
            ps.setInt(2, p.getStudentId());
            ps.setDouble(3, p.getAmount());
            ps.setString(4, p.getTerm());
            // paymentDate = when the money actually changed hands (user-editable)
            ps.setTimestamp(5, Timestamp.valueOf(p.getPaymentDate()));
            // recordedAt = when it was entered here (always now, never editable)
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(7, p.getRecordedByUserId());
            ps.setString(8, p.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert payment: " + e.getMessage(), e);
        }
        return -1;
    }

    /** Corrects the transaction date of an existing payment. Never touches recordedAt. */
    public void updateTransactionDate(int paymentId, LocalDateTime newDate) {
        String sql = "UPDATE Payments SET paymentDate = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(newDate));
            ps.setInt(2, paymentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update payment date: " + e.getMessage(), e);
        }
    }

    public List<Payment> findByStudent(int studentId) {
        List<Payment> results = new ArrayList<>();
        String sql = "SELECT * FROM Payments WHERE studentId = ? ORDER BY paymentDate";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch payments: " + e.getMessage(), e);
        }
        return results;
    }

    public List<Payment> findAll() {
        List<Payment> results = new ArrayList<>();
        String sql = "SELECT * FROM Payments ORDER BY paymentDate DESC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) results.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all payments: " + e.getMessage(), e);
        }
        return results;
    }

    public double totalCollected() {
        String sql = "SELECT SUM(amount) AS total FROM Payments";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble("total");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to sum total collected: " + e.getMessage(), e);
        }
        return 0.0;
    }

    private Payment map(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId(rs.getInt("id"));
        p.setReceiptNumber(rs.getInt("receiptNumber"));
        p.setStudentId(rs.getInt("studentId"));
        p.setAmount(rs.getDouble("amount"));
        p.setTerm(rs.getString("term"));
        Timestamp ts = rs.getTimestamp("paymentDate");
        p.setPaymentDate(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
        Timestamp rec = rs.getTimestamp("recordedAt");
        p.setRecordedAt(rec != null ? rec.toLocalDateTime() : null);
        p.setRecordedByUserId(rs.getInt("recordedByUserId"));
        p.setNotes(rs.getString("notes"));
        return p;
    }
}