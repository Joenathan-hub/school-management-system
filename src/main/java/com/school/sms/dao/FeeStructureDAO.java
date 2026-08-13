package com.school.sms.dao;

import com.school.sms.model.FeeStructure;
import com.school.sms.util.DatabaseManager;

import java.sql.*;

public class FeeStructureDAO {

    public int insert(FeeStructure f) {
        String sql = "INSERT INTO FeeStructures (studentId, term, baseFee, discount, discountReason, broughtForward) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, f.getStudentId());
            ps.setString(2, f.getTerm());
            ps.setDouble(3, f.getBaseFee());
            ps.setDouble(4, f.getDiscount());
            ps.setString(5, f.getDiscountReason());
            ps.setDouble(6, f.getBroughtForward());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert fee structure: " + e.getMessage(), e);
        }
        return -1;
    }

    public FeeStructure findByStudentAndTerm(int studentId, String term) {
        String sql = "SELECT * FROM FeeStructures WHERE studentId = ? AND term = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, term);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    FeeStructure f = new FeeStructure();
                    f.setId(rs.getInt("id"));
                    f.setStudentId(rs.getInt("studentId"));
                    f.setTerm(rs.getString("term"));
                    f.setBaseFee(rs.getDouble("baseFee"));
                    f.setDiscount(rs.getDouble("discount"));
                    f.setDiscountReason(rs.getString("discountReason"));
                    f.setBroughtForward(rs.getDouble("broughtForward"));
                    return f;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch fee structure: " + e.getMessage(), e);
        }
        return null;
    }

    /** Total amount already paid by this student for this term (used to compute balance). */
    public double totalPaid(int studentId, String term) {
        String sql = "SELECT SUM(amount) AS total FROM Payments WHERE studentId = ? AND term = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, term);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to sum payments: " + e.getMessage(), e);
        }
        return 0.0;
    }

    /** Used by Term Management when starting a new term — only ever touches broughtForward. */
    public void upsertBroughtForward(int studentId, String term, double broughtForward) {
        FeeStructure existing = findByStudentAndTerm(studentId, term);
        if (existing == null) {
            String sql = "INSERT INTO FeeStructures (studentId, term, baseFee, discount, discountReason, broughtForward) VALUES (?, ?, 0, 0, NULL, ?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, studentId);
                ps.setString(2, term);
                ps.setDouble(3, broughtForward);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to carry forward balance: " + e.getMessage(), e);
            }
        } else {
            String sql = "UPDATE FeeStructures SET broughtForward = ? WHERE id = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, broughtForward);
                ps.setInt(2, existing.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update carried-forward balance: " + e.getMessage(), e);
            }
        }
    }

    /** Used by the Set Term Fees screen — only ever touches baseFee/discount, never broughtForward. */
    public void upsertBaseFeeAndDiscount(int studentId, String term, double baseFee, double discount, String discountReason) {
        FeeStructure existing = findByStudentAndTerm(studentId, term);
        if (existing == null) {
            String sql = "INSERT INTO FeeStructures (studentId, term, baseFee, discount, discountReason, broughtForward) VALUES (?, ?, ?, ?, ?, 0)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, studentId);
                ps.setString(2, term);
                ps.setDouble(3, baseFee);
                ps.setDouble(4, discount);
                ps.setString(5, discountReason);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to set fee structure: " + e.getMessage(), e);
            }
        } else {
            String sql = "UPDATE FeeStructures SET baseFee = ?, discount = ?, discountReason = ? WHERE id = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, baseFee);
                ps.setDouble(2, discount);
                ps.setString(3, discountReason);
                ps.setInt(4, existing.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update fee structure: " + e.getMessage(), e);
            }
        }
    }
}
