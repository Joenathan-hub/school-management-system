package com.school.sms.service;

import com.school.sms.dao.AuditLogDAO;
import com.school.sms.dao.PasswordResetDAO;
import com.school.sms.dao.UserDAO;
import com.school.sms.model.Role;
import com.school.sms.model.User;
import com.school.sms.util.PasswordUtil;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final PasswordResetDAO resetDAO = new PasswordResetDAO();
    private final AuditLogDAO auditLog = new AuditLogDAO();

    /** Returns the logged-in User, or null if credentials are invalid. */
    public User login(String username, String plainPassword) {
        User user = userDAO.findByUsername(username);
        if (user == null || !user.isActive()) return null;
        if (!PasswordUtil.verify(plainPassword, user.getPasswordHash())) return null;
        auditLog.log(user.getId(), "LOGIN", "User logged in");
        return user;
    }

    /**
     * Administrator generates a one-time reset code for a user (bursar, teacher,
     * or another admin) who is locked out or wants to change their password.
     * Returns the plain code — this is the ONLY time it's available in plain
     * text, so the admin must hand it to the user directly (in person, etc).
     */
    public String issueResetCode(int adminUserId, int targetUserId) {
        String plainCode = PasswordUtil.generateResetCode();
        resetDAO.createCode(targetUserId, PasswordUtil.hash(plainCode));
        auditLog.log(adminUserId, "ISSUE_RESET_CODE", "Issued reset code for user #" + targetUserId);
        return plainCode;
    }

    /**
     * User redeems the code (given by the admin) to set a brand-new password.
     * Works for self-service resets by ANY role — admin and bursar both use
     * this same flow, they just need an admin to generate the code first.
     */
    public boolean redeemResetCode(int userId, String plainCode, String newPassword) {
        PasswordResetDAO.ResetCodeRecord record = resetDAO.findLatestUnused(userId);
        if (record == null) return false;
        if (!PasswordUtil.verify(plainCode, record.codeHash)) return false;

        userDAO.updatePasswordHash(userId, PasswordUtil.hash(newPassword));
        resetDAO.markUsed(record.id);
        auditLog.log(userId, "PASSWORD_RESET", "Password reset via admin-issued code");
        return true;
    }

    /** Direct self-service change when the user already knows their current password. */
    public boolean changePassword(User user, String currentPassword, String newPassword) {
        if (!PasswordUtil.verify(currentPassword, user.getPasswordHash())) return false;
        userDAO.updatePasswordHash(user.getId(), PasswordUtil.hash(newPassword));
        auditLog.log(user.getId(), "PASSWORD_CHANGE", "User changed their own password");
        return true;
    }
}
