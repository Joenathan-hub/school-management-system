package com.school.sms.util;

import org.mindrot.jbcrypt.BCrypt;
import java.security.SecureRandom;

public class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String RESET_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I to avoid confusion

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    public static boolean verify(String plainPassword, String hash) {
        return BCrypt.checkpw(plainPassword, hash);
    }

    /**
     * Generates an 8-character one-time reset code that the administrator
     * hands to a user (bursar or another admin) whose password needs resetting.
     * The code itself should be stored hashed in the DB, same as a password,
     * and marked used/expired once redeemed.
     */
    public static String generateResetCode() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(RESET_CODE_CHARS.charAt(RANDOM.nextInt(RESET_CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
