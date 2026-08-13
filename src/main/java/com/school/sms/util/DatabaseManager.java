package com.school.sms.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the connection to the MS Access (.accdb) database and creates the
 * full schema on first run. Same approach as First Bank Uganda: UCanAccess
 * can create a brand-new .accdb file via the `newdatabaseversion` parameter,
 * so no template file is required.
 */
public class DatabaseManager {

    // Store the DB in the user's own profile folder — NOT alongside the app.
    // Program Files (where the app installs to) is protected by Windows; a
    // normal, non-admin process can't create files there. Writing here
    // instead means the app works correctly without needing admin rights.
    public static final String APP_DATA_DIR = System.getProperty("user.home") + java.io.File.separator + "SchoolManagementSystem";
    public static final String DB_PATH = APP_DATA_DIR + java.io.File.separator + "school_management.accdb";
    private static final String URL = "jdbc:ucanaccess://" + DB_PATH + ";newdatabaseversion=V2010";

    static {
        java.io.File dir = new java.io.File(APP_DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            boolean isNewDatabase = !new File(DB_PATH).exists();
            connection = DriverManager.getConnection(URL);
            if (isNewDatabase) {
                createSchema(connection);
            }
            runMigrations(connection);
        }
        return connection;
    }

    /**
     * Safe, additive schema upgrades for databases created by earlier
     * versions of the app. Each check is a no-op if already applied, so
     * this runs on every startup with no risk to existing data.
     */
    private static void runMigrations(Connection conn) {
        tryExecute(conn, """
            CREATE TABLE Terms (
                id COUNTER PRIMARY KEY,
                name TEXT(30) NOT NULL,
                status TEXT(10) NOT NULL,
                startDate DATETIME,
                endDate DATETIME
            )
        """);

        tryExecute(conn, "ALTER TABLE FeeStructures ADD COLUMN broughtForward CURRENCY DEFAULT 0");

        tryExecute(conn, "ALTER TABLE Students ADD COLUMN boardingStatus TEXT(10) DEFAULT 'Day'");

        tryExecute(conn, """
            CREATE TABLE RequirementTemplates (
                id COUNTER PRIMARY KEY,
                category TEXT(10) NOT NULL,
                itemName TEXT(100) NOT NULL
            )
        """);

        tryExecute(conn, """
            CREATE TABLE SchoolEvents (
                id COUNTER PRIMARY KEY,
                title TEXT(150) NOT NULL,
                eventDate DATETIME,
                description TEXT(255),
                createdByUserId LONG
            )
        """);
    }

    /** Attempts a schema change and silently ignores failure — the only realistic failure here is "already applied", which is expected on every startup after the first. */
    private static void tryExecute(Connection conn, String sql) {
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            // Already exists — fine, that's the normal case.
        }
    }

    private static void createSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {

            st.execute("""
                CREATE TABLE Users (
                    id COUNTER PRIMARY KEY,
                    username TEXT(50) NOT NULL,
                    passwordHash TEXT(255) NOT NULL,
                    role TEXT(20) NOT NULL,
                    fullName TEXT(100),
                    assignedClass TEXT(30),
                    active YESNO DEFAULT TRUE
                )
            """);

            st.execute("""
                CREATE TABLE PasswordResetCodes (
                    id COUNTER PRIMARY KEY,
                    userId LONG NOT NULL,
                    codeHash TEXT(255) NOT NULL,
                    createdAt DATETIME,
                    used YESNO DEFAULT FALSE
                )
            """);

            st.execute("""
                CREATE TABLE Guardians (
                    id COUNTER PRIMARY KEY,
                    fullName TEXT(100) NOT NULL,
                    relationship TEXT(20),
                    occupation TEXT(100),
                    contact TEXT(30)
                )
            """);

            st.execute("""
                CREATE TABLE Students (
                    id COUNTER PRIMARY KEY,
                    studentId TEXT(30) NOT NULL,
                    fullName TEXT(100) NOT NULL,
                    age INTEGER,
                    sex TEXT(10),
                    studentClass TEXT(30),
                    admissionDate DATETIME,
                    fatherId LONG,
                    motherId LONG,
                    active YESNO DEFAULT TRUE
                )
            """);

            st.execute("""
                CREATE TABLE FeeStructures (
                    id COUNTER PRIMARY KEY,
                    studentId LONG NOT NULL,
                    term TEXT(30) NOT NULL,
                    baseFee CURRENCY,
                    discount CURRENCY DEFAULT 0,
                    discountReason TEXT(150)
                )
            """);

            st.execute("""
                CREATE TABLE Payments (
                    id COUNTER PRIMARY KEY,
                    receiptNumber LONG NOT NULL,
                    studentId LONG NOT NULL,
                    amount CURRENCY NOT NULL,
                    term TEXT(30),
                    paymentDate DATETIME,
                    recordedByUserId LONG,
                    notes TEXT(150)
                )
            """);

            st.execute("""
                CREATE TABLE Workers (
                    id COUNTER PRIMARY KEY,
                    workerId TEXT(30) NOT NULL,
                    fullName TEXT(100) NOT NULL,
                    jobTitle TEXT(50),
                    contact TEXT(30),
                    dateJoined DATETIME,
                    monthlySalary CURRENCY,
                    active YESNO DEFAULT TRUE
                )
            """);

            st.execute("""
                CREATE TABLE WorkerPayments (
                    id COUNTER PRIMARY KEY,
                    workerId LONG NOT NULL,
                    amount CURRENCY NOT NULL,
                    paymentDate DATETIME,
                    forMonth TEXT(20),
                    recordedByUserId LONG,
                    notes TEXT(150)
                )
            """);

            st.execute("""
                CREATE TABLE Expenditures (
                    id COUNTER PRIMARY KEY,
                    description TEXT(150) NOT NULL,
                    amount CURRENCY NOT NULL,
                    category TEXT(50),
                    expenditureDate DATETIME,
                    recordedByUserId LONG
                )
            """);

            st.execute("""
                CREATE TABLE Requirements (
                    id COUNTER PRIMARY KEY,
                    studentId LONG NOT NULL,
                    term TEXT(30),
                    itemName TEXT(100),
                    brought YESNO DEFAULT FALSE
                )
            """);

            st.execute("""
                CREATE TABLE Uniforms (
                    id COUNTER PRIMARY KEY,
                    studentId LONG NOT NULL,
                    term TEXT(30) NOT NULL,
                    itemName TEXT(100) NOT NULL,
                    received YESNO DEFAULT FALSE,
                    dateReceived DATETIME,
                    recordedByUserId LONG
                )
            """);

            st.execute("""
                CREATE TABLE Results (
                    id COUNTER PRIMARY KEY,
                    studentId LONG NOT NULL,
                    term TEXT(30),
                    subject TEXT(50),
                    marks INTEGER,
                    grade TEXT(5),
                    comment TEXT(150),
                    enteredByUserId LONG
                )
            """);

            st.execute("""
                CREATE TABLE Attendance (
                    id COUNTER PRIMARY KEY,
                    studentId LONG NOT NULL,
                    term TEXT(30),
                    returned YESNO,
                    recordedByUserId LONG
                )
            """);

            st.execute("""
                CREATE TABLE SmsQueue (
                    id COUNTER PRIMARY KEY,
                    recipientContact TEXT(30) NOT NULL,
                    message TEXT(255) NOT NULL,
                    createdAt DATETIME,
                    sent YESNO DEFAULT FALSE,
                    sentAt DATETIME,
                    failureReason TEXT(150)
                )
            """);

            st.execute("""
                CREATE TABLE AuditLog (
                    id COUNTER PRIMARY KEY,
                    userId LONG,
                    action TEXT(100),
                    details TEXT(255),
                    timestamp DATETIME
                )
            """);

            st.execute("""
                CREATE TABLE Settings (
                    settingKey TEXT(50) PRIMARY KEY,
                    settingValue TEXT(255)
                )
            """);
        }
    }

    private DatabaseManager() {}
}
