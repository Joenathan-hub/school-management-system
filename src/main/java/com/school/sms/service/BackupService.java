package com.school.sms.service;

import com.school.sms.dao.AuditLogDAO;
import com.school.sms.dao.SettingsDAO;
import com.school.sms.util.DatabaseManager;
import com.school.sms.util.DateTimeUtil;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Manual backup always works regardless of connectivity — it's just a local
 * file copy. Automatic backup (maybeAutoBackup) additionally requires being
 * online and not having already auto-backed-up today.
 */
public class BackupService {

    private static final String BACKUP_DIR = DatabaseManager.APP_DATA_DIR + java.io.File.separator + "backups";

    private final SettingsDAO settingsDAO = new SettingsDAO();

    public String backupNow() throws IOException {
        Path source = Path.of(DatabaseManager.DB_PATH);
        if (!Files.exists(source)) {
            throw new IOException("Database file not found at " + source.toAbsolutePath());
        }

        Path backupDir = Path.of(BACKUP_DIR);
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }

        String timestamp = LocalDateTime.now().format(DateTimeUtil.DATE_TIME)
                .replace("/", "-").replace(":", "-").replace(" ", "_");
        Path destination = backupDir.resolve("school_management_backup_" + timestamp + ".accdb");

        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

        settingsDAO.set("last.backup.at", LocalDateTime.now().format(DateTimeUtil.DATE_TIME));

        return destination.toAbsolutePath().toString();
    }

    public String getLastBackupTime() {
        return settingsDAO.get("last.backup.at", "Never");
    }

    /**
     * Called after login and periodically while the app is open. Silently
     * does nothing if offline or already auto-backed-up today — safe to
     * call as often as you like.
     */
    public String maybeAutoBackup(int userId) {
        if (!new ConnectivityService().isOnline()) {
            return null;
        }

        String today = LocalDate.now().toString();
        String lastAutoDate = settingsDAO.get("last.auto.backup.date", "");
        if (today.equals(lastAutoDate)) {
            return null;
        }

        try {
            String path = backupNow();
            settingsDAO.set("last.auto.backup.date", today);
            new AuditLogDAO().log(userId, "AUTO_BACKUP", "Automatic backup completed: " + path);
            return path;
        } catch (IOException e) {
            return null;
        }
    }
}