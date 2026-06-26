package com.portfolio.siemlite.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppDataPathServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesDatabasePathUnderAppDataDirectory() {
        AppDataPathService service = new AppDataPathService(tempDir);

        Path databasePath = service.getDatabasePath();

        assertEquals(tempDir.resolve("SIEM Lite").resolve("data").resolve("siem-lite.db"), databasePath);
    }

    @Test
    void createsDataDirectory() throws IOException {
        AppDataPathService service = new AppDataPathService(tempDir);

        Path dataDirectory = service.createDataDirectory();

        assertTrue(Files.exists(dataDirectory));
        assertTrue(Files.isDirectory(dataDirectory));
    }

    @Test
    void resolvesSettingsPathUnderConfigDirectory() {
        AppDataPathService service = new AppDataPathService(tempDir);

        Path settingsPath = service.getSettingsPath();

        assertEquals(tempDir.resolve("SIEM Lite").resolve("config").resolve("settings.properties"), settingsPath);
    }

    @Test
    void createsConfigDirectory() throws IOException {
        AppDataPathService service = new AppDataPathService(tempDir);

        Path configDirectory = service.createConfigDirectory();

        assertTrue(Files.exists(configDirectory));
        assertTrue(Files.isDirectory(configDirectory));
    }
}
