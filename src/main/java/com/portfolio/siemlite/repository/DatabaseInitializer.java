package com.portfolio.siemlite.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public void initialize(Path databasePath) {
        try {
            Path parent = databasePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Connection connection = DriverManager.getConnection(jdbcUrl(databasePath));
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS saved_events (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            line_number INTEGER NOT NULL,
                            event_timestamp TEXT,
                            severity TEXT NOT NULL,
                            source TEXT,
                            message TEXT NOT NULL,
                            raw_line TEXT NOT NULL,
                            suspicious INTEGER NOT NULL,
                            matched_keyword TEXT,
                            imported_file_name TEXT NOT NULL,
                            imported_file_path TEXT,
                            saved_at TEXT NOT NULL,
                            content_hash TEXT NOT NULL UNIQUE
                        )
                        """);
                statement.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_saved_events_saved_at
                        ON saved_events(saved_at)
                        """);
                statement.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_saved_events_severity
                        ON saved_events(severity)
                        """);
                statement.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_saved_events_keyword
                        ON saved_events(matched_keyword)
                        """);
            }
        } catch (IOException | SQLException exception) {
            throw new DatabaseException("Could not initialize local SQLite database.", exception);
        }
    }

    static String jdbcUrl(Path databasePath) {
        return "jdbc:sqlite:" + databasePath.toAbsolutePath();
    }
}
