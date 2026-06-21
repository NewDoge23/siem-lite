package com.portfolio.siemlite.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseInitializerTest {

    @TempDir
    Path tempDir;

    private final DatabaseInitializer initializer = new DatabaseInitializer();

    @Test
    void createsDatabaseFileAndSchema() throws Exception {
        Path databasePath = tempDir.resolve("data").resolve("test.db");

        initializer.initialize(databasePath);

        assertTrue(Files.exists(databasePath));
        try (Connection connection = DriverManager.getConnection(DatabaseInitializer.jdbcUrl(databasePath));
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT name FROM sqlite_master
                     WHERE type = 'table' AND name = 'saved_events'
                     """)) {
            assertTrue(resultSet.next());
            assertEquals("saved_events", resultSet.getString("name"));
        }
    }

    @Test
    void canRunInitializationMoreThanOnce() {
        Path databasePath = tempDir.resolve("data").resolve("test.db");

        initializer.initialize(databasePath);
        initializer.initialize(databasePath);

        assertTrue(Files.exists(databasePath));
    }
}
