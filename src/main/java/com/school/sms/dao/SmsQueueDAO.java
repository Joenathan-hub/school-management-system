package com.school.sms.dao;

import com.school.sms.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SmsQueueDAO {

    public static class QueuedSms {
        public int id;
        public String recipientContact;
        public String message;
    }

    /** Adds a message to the queue. Does NOT send it — SmsService handles sending. */
    public void enqueue(String recipientContact, String message) {
        String sql = "INSERT INTO SmsQueue (recipientContact, message, createdAt, sent) VALUES (?, ?, ?, FALSE)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, recipientContact);
            ps.setString(2, message);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to enqueue SMS: " + e.getMessage());
        }
    }

    public List<QueuedSms> getPending() {
        List<QueuedSms> list = new ArrayList<>();
        String sql = "SELECT id, recipientContact, message FROM SmsQueue WHERE sent = FALSE";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                QueuedSms q = new QueuedSms();
                q.id = rs.getInt("id");
                q.recipientContact = rs.getString("recipientContact");
                q.message = rs.getString("message");
                list.add(q);
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch pending SMS: " + e.getMessage());
        }
        return list;
    }

    public void markSent(int id) {
        updateStatus(id, true, null);
    }

    public void markFailed(int id, String reason) {
        updateStatus(id, false, reason);
    }

    private void updateStatus(int id, boolean sent, String failureReason) {
        String sql = "UPDATE SmsQueue SET sent = ?, sentAt = ?, failureReason = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, sent);
            ps.setTimestamp(2, sent ? Timestamp.valueOf(LocalDateTime.now()) : null);
            ps.setString(3, failureReason);
            ps.setInt(4, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to update SMS status: " + e.getMessage());
        }
    }
}
