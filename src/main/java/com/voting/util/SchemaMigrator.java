package com.voting.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;

public final class SchemaMigrator {
    private SchemaMigrator() {
    }

    public static void run() throws SQLException {
        System.out.println("[SchemaMigrator] Starting migrations...");
        try (Connection connection = DatabaseUtil.getConnection()) {
            // users table migrations
            migrate(connection, "users", "mobile_number",    "ALTER TABLE users ADD COLUMN mobile_number VARCHAR(20)");
            migrate(connection, "users", "voter_id_number",  "ALTER TABLE users ADD COLUMN voter_id_number VARCHAR(50) UNIQUE");
            migrate(connection, "users", "date_of_birth",    "ALTER TABLE users ADD COLUMN date_of_birth DATE");
            migrate(connection, "users", "age",              "ALTER TABLE users ADD COLUMN age INT");
            migrate(connection, "users", "election_center",  "ALTER TABLE users ADD COLUMN election_center VARCHAR(150)");
            migrate(connection, "users", "city",             "ALTER TABLE users ADD COLUMN city VARCHAR(100)");
            migrate(connection, "users", "state",            "ALTER TABLE users ADD COLUMN state VARCHAR(100)");
            migrate(connection, "users", "profile_photo_path","ALTER TABLE users ADD COLUMN profile_photo_path VARCHAR(255)");
            migrate(connection, "users", "otp_code",         "ALTER TABLE users ADD COLUMN otp_code VARCHAR(6)");
            migrate(connection, "users", "otp_expiry",       "ALTER TABLE users ADD COLUMN otp_expiry TIMESTAMP NULL");
            migrate(connection, "users", "failed_attempts",   "ALTER TABLE users ADD COLUMN failed_attempts INT DEFAULT 0");
            migrate(connection, "users", "locked_until",     "ALTER TABLE users ADD COLUMN locked_until TIMESTAMP NULL");
            allowAdminRoleLevels(connection);

            // elections table migrations
            migrate(connection, "elections", "election_date","ALTER TABLE elections ADD COLUMN election_date DATE NOT NULL DEFAULT '2000-01-01'");
            migrate(connection, "elections", "start_time",   "ALTER TABLE elections ADD COLUMN start_time TIME NOT NULL DEFAULT '00:00:00'");
            migrate(connection, "elections", "end_time",     "ALTER TABLE elections ADD COLUMN end_time TIME NOT NULL DEFAULT '23:59:59'");
            migrate(connection, "elections", "results_announced", "ALTER TABLE elections ADD COLUMN results_announced BOOLEAN NOT NULL DEFAULT FALSE AFTER end_time");
            migrate(connection, "elections", "nomination_starts_at", "ALTER TABLE elections ADD COLUMN nomination_starts_at TIMESTAMP NULL AFTER results_announced");
            migrate(connection, "elections", "nomination_ends_at", "ALTER TABLE elections ADD COLUMN nomination_ends_at TIMESTAMP NULL AFTER nomination_starts_at");
            migrate(connection, "elections", "ready_for_results", "ALTER TABLE elections ADD COLUMN ready_for_results BOOLEAN NOT NULL DEFAULT FALSE AFTER nomination_ends_at");

            // candidates table migrations
            migrate(connection, "candidates", "photo_path",  "ALTER TABLE candidates ADD COLUMN photo_path VARCHAR(255) NULL");
            migrate(connection, "candidates", "ballot_order", "ALTER TABLE candidates ADD COLUMN ballot_order INT NOT NULL DEFAULT 0 AFTER photo_path");

            // votes table migrations
            migrate(connection, "votes", "receipt_id",       "ALTER TABLE votes ADD COLUMN receipt_id VARCHAR(40) NULL UNIQUE AFTER candidate_id");
            migrate(connection, "votes", "election_id",      "ALTER TABLE votes ADD COLUMN election_id INT NULL AFTER user_id");
            migrate(connection, "votes", "created_at",       "ALTER TABLE votes ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER receipt_id");
            migrate(connection, "votes", "ip_address",       "ALTER TABLE votes ADD COLUMN ip_address VARCHAR(45)");
            migrate(connection, "votes", "user_agent",       "ALTER TABLE votes ADD COLUMN user_agent TEXT");
            migrate(connection, "votes", "digital_signature","ALTER TABLE votes ADD COLUMN digital_signature VARCHAR(255)");
            migrateVotesElectionId(connection);
            addVoteUserForeignKeySupportIndex(connection);
            dropSingleColumnUserVoteUniqueIndexes(connection);
            addUserElectionVoteUniqueIndex(connection);
            createFeatureTables(connection);
            migrate(connection, "candidate_nominations", "document_status", "ALTER TABLE candidate_nominations ADD COLUMN document_status ENUM('PENDING', 'VERIFIED', 'REJECTED') NOT NULL DEFAULT 'PENDING' AFTER admin_note");
            migrate(connection, "candidate_nominations", "document_note", "ALTER TABLE candidate_nominations ADD COLUMN document_note VARCHAR(500) AFTER document_status");
            
            // Emergency: Reset all lockouts to fix universal lockout bug
            new com.voting.dao.UserDao().unlockAllAccounts();
        }
        System.out.println("[SchemaMigrator] Migrations completed.");
    }

    private static void allowAdminRoleLevels(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE users MODIFY COLUMN role ENUM('super_admin', 'admin', 'election_officer', 'auditor', 'user') NOT NULL DEFAULT 'user'");
        } catch (SQLException e) {
            System.err.println("[SchemaMigrator] Failed to widen user role enum: " + e.getMessage());
        }
    }

    private static void createFeatureTables(Connection connection) {
        String eligibility = "CREATE TABLE IF NOT EXISTS election_eligibility (" +
                "election_id INT NOT NULL, " +
                "user_id INT NOT NULL, " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (election_id, user_id), " +
                "CONSTRAINT fk_eligibility_election FOREIGN KEY (election_id) REFERENCES elections(id) ON DELETE CASCADE, " +
                "CONSTRAINT fk_eligibility_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";
        String nominations = "CREATE TABLE IF NOT EXISTS candidate_nominations (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "user_id INT NOT NULL, " +
                "election_id INT NOT NULL, " +
                "manifesto TEXT NOT NULL, " +
                "status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING', " +
                "admin_note VARCHAR(500), " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "reviewed_at TIMESTAMP NULL, " +
                "UNIQUE KEY uq_nomination_user_election (user_id, election_id), " +
                "CONSTRAINT fk_nomination_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                "CONSTRAINT fk_nomination_election FOREIGN KEY (election_id) REFERENCES elections(id) ON DELETE CASCADE)";
        String notifications = "CREATE TABLE IF NOT EXISTS notifications (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "user_id INT NULL, " +
                "title VARCHAR(150) NOT NULL, " +
                "message VARCHAR(1000) NOT NULL, " +
                "type VARCHAR(40) NOT NULL DEFAULT 'info', " +
                "read_at TIMESTAMP NULL, " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "INDEX idx_notifications_user_created (user_id, created_at), " +
                "CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";
        String officerNotes = "CREATE TABLE IF NOT EXISTS officer_notes (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "election_id INT NOT NULL, " +
                "officer_id INT NOT NULL, " +
                "note VARCHAR(1000) NOT NULL, " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "CONSTRAINT fk_officer_note_election FOREIGN KEY (election_id) REFERENCES elections(id) ON DELETE CASCADE, " +
                "CONSTRAINT fk_officer_note_user FOREIGN KEY (officer_id) REFERENCES users(id) ON DELETE CASCADE)";
        String incidents = "CREATE TABLE IF NOT EXISTS election_incidents (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "election_id INT NOT NULL, " +
                "officer_id INT NOT NULL, " +
                "severity ENUM('LOW', 'MEDIUM', 'HIGH') NOT NULL DEFAULT 'LOW', " +
                "category VARCHAR(80) NOT NULL, " +
                "description VARCHAR(1000) NOT NULL, " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "CONSTRAINT fk_incident_election FOREIGN KEY (election_id) REFERENCES elections(id) ON DELETE CASCADE, " +
                "CONSTRAINT fk_incident_user FOREIGN KEY (officer_id) REFERENCES users(id) ON DELETE CASCADE)";
        String dryRuns = "CREATE TABLE IF NOT EXISTS election_dry_runs (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "election_id INT NOT NULL, " +
                "officer_id INT NOT NULL, " +
                "result VARCHAR(40) NOT NULL, " +
                "details VARCHAR(1000) NOT NULL, " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "CONSTRAINT fk_dry_run_election FOREIGN KEY (election_id) REFERENCES elections(id) ON DELETE CASCADE, " +
                "CONSTRAINT fk_dry_run_user FOREIGN KEY (officer_id) REFERENCES users(id) ON DELETE CASCADE)";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(eligibility);
            statement.executeUpdate(nominations);
            statement.executeUpdate(notifications);
            statement.executeUpdate(officerNotes);
            statement.executeUpdate(incidents);
            statement.executeUpdate(dryRuns);
        } catch (SQLException e) {
            System.err.println("[SchemaMigrator] Failed to create feature tables: " + e.getMessage());
        }
    }

    private static void migrate(Connection connection, String table, String column, String sql) {
        try {
            if (!columnExists(connection, table, column)) {
                System.out.println("[SchemaMigrator] Adding column " + column + " to " + table);
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(sql);
                }
            }
        } catch (SQLException e) {
            System.err.println("[SchemaMigrator] Failed to migrate " + table + "." + column + ": " + e.getMessage());
        }
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        String catalog = connection.getCatalog();
        try (ResultSet columns = connection.getMetaData().getColumns(catalog, null, tableName, columnName)) {
            if (columns.next()) return true;
        }
        // Try uppercase
        try (ResultSet columns = connection.getMetaData().getColumns(catalog, null, tableName.toUpperCase(), columnName.toUpperCase())) {
            return columns.next();
        }
    }

    private static void migrateVotesElectionId(Connection connection) throws SQLException {
        String sql = "UPDATE votes v JOIN candidates c ON c.id = v.candidate_id " +
                "SET v.election_id = c.election_id WHERE v.election_id IS NULL";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void dropSingleColumnUserVoteUniqueIndexes(Connection connection) throws SQLException {
        String sql = "SELECT INDEX_NAME, COUNT(*) AS column_count, " +
                "SUM(CASE WHEN COLUMN_NAME = 'user_id' THEN 1 ELSE 0 END) AS user_column_count " +
                "FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'votes' AND NON_UNIQUE = 0 " +
                "GROUP BY INDEX_NAME " +
                "HAVING column_count = 1 AND user_column_count = 1";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (!"PRIMARY".equalsIgnoreCase(indexName)) {
                    try (Statement statement = connection.createStatement()) {
                        System.out.println("[SchemaMigrator] Dropping old single-user vote unique index " + indexName);
                        statement.executeUpdate("ALTER TABLE votes DROP INDEX `" + indexName + "`");
                    }
                }
            }
        }
    }

    private static void addVoteUserForeignKeySupportIndex(Connection connection) {
        try {
            if (!indexExists(connection, "votes", "idx_votes_user_fk")) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE votes ADD INDEX idx_votes_user_fk (user_id, id)");
                }
            }
        } catch (SQLException e) {
            System.err.println("[SchemaMigrator] Failed to add votes user FK support index: " + e.getMessage());
        }
    }

    private static void addUserElectionVoteUniqueIndex(Connection connection) {
        try {
            if (!indexExists(connection, "votes", "uq_votes_user_election")) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE votes ADD UNIQUE KEY uq_votes_user_election (user_id, election_id)");
                }
            }
        } catch (SQLException e) {
            System.err.println("[SchemaMigrator] Failed to add votes user/election unique index: " + e.getMessage());
        }
    }

    private static boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        try (ResultSet indexes = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            while (indexes.next()) {
                if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
