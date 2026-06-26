package com.portfolio.siemlite.config;

import com.portfolio.siemlite.localization.LanguageOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsDefaultSettingsWhenFileDoesNotExist() {
        SettingsService service = new SettingsService(new AppDataPathService(tempDir));

        UserSettings settings = service.load();

        assertEquals(LanguageOption.ENGLISH, settings.language());
    }

    @Test
    void savesAndLoadsLanguagePreference() {
        AppDataPathService pathService = new AppDataPathService(tempDir);
        SettingsService service = new SettingsService(pathService);

        service.save(new UserSettings(LanguageOption.SPANISH));
        UserSettings settings = service.load();

        assertTrue(Files.exists(pathService.getSettingsPath()));
        assertEquals(LanguageOption.SPANISH, settings.language());
    }

    @Test
    void returnsDefaultSettingsForInvalidLanguageCode() throws IOException {
        AppDataPathService pathService = new AppDataPathService(tempDir);
        pathService.createConfigDirectory();
        Files.writeString(pathService.getSettingsPath(), "language=unknown");
        SettingsService service = new SettingsService(pathService);

        UserSettings settings = service.load();

        assertEquals(LanguageOption.ENGLISH, settings.language());
    }
}
