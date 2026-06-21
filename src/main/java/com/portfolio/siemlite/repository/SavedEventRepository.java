package com.portfolio.siemlite.repository;

import com.portfolio.siemlite.model.SavedLogEvent;
import com.portfolio.siemlite.model.Severity;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SavedEventRepository {

    private final Path databasePath;

    public SavedEventRepository(Path databasePath) {
        this.databasePath = databasePath;
    }

    public boolean save(SavedLogEvent event) {
        String sql = """
                INSERT OR IGNORE INTO saved_events (
                    line_number,
                    event_timestamp,
                    severity,
                    source,
                    message,
                    raw_line,
                    suspicious,
                    matched_keyword,
                    imported_file_name,
                    imported_file_path,
                    saved_at,
                    content_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, event.getLineNumber());
            statement.setString(2, event.getTimestamp());
            statement.setString(3, event.getSeverity().name());
            statement.setString(4, event.getSource());
            statement.setString(5, event.getMessage());
            statement.setString(6, event.getRawLine());
            statement.setInt(7, event.isSuspicious() ? 1 : 0);
            statement.setString(8, event.getMatchedKeyword());
            statement.setString(9, event.getImportedFileName());
            statement.setString(10, event.getImportedFilePath());
            statement.setString(11, event.getSavedAt());
            statement.setString(12, event.getContentHash());

            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not save event to local SQLite database.", exception);
        }
    }

    public List<SavedLogEvent> findAll() {
        String sql = """
                SELECT
                    id,
                    line_number,
                    event_timestamp,
                    severity,
                    source,
                    message,
                    raw_line,
                    suspicious,
                    matched_keyword,
                    imported_file_name,
                    imported_file_path,
                    saved_at,
                    content_hash
                FROM saved_events
                ORDER BY id DESC
                """;

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            List<SavedLogEvent> events = new ArrayList<>();
            while (resultSet.next()) {
                events.add(mapRow(resultSet));
            }
            return events;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load saved events from local SQLite database.", exception);
        }
    }

    public int count() {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM saved_events")) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not count saved events in local SQLite database.", exception);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(DatabaseInitializer.jdbcUrl(databasePath));
    }

    private SavedLogEvent mapRow(ResultSet resultSet) throws SQLException {
        return new SavedLogEvent(
                resultSet.getLong("id"),
                resultSet.getInt("line_number"),
                resultSet.getString("event_timestamp"),
                Severity.valueOf(resultSet.getString("severity")),
                resultSet.getString("source"),
                resultSet.getString("message"),
                resultSet.getString("raw_line"),
                resultSet.getInt("suspicious") == 1,
                resultSet.getString("matched_keyword"),
                resultSet.getString("imported_file_name"),
                resultSet.getString("imported_file_path"),
                resultSet.getString("saved_at"),
                resultSet.getString("content_hash"));
    }
}
