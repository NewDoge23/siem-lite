package com.portfolio.siemlite.config;

import com.portfolio.siemlite.localization.LanguageOption;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class SettingsService {

    private static final String LANGUAGE_KEY = "language";

    private final AppDataPathService appDataPathService;

    public SettingsService(AppDataPathService appDataPathService) {
        this.appDataPathService = appDataPathService;
    }

    public UserSettings load() {
        Path settingsPath = appDataPathService.getSettingsPath();
        if (!Files.exists(settingsPath)) {
            return UserSettings.defaults();
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(settingsPath)) {
            properties.load(inputStream);
            return new UserSettings(LanguageOption.fromCode(properties.getProperty(LANGUAGE_KEY)));
        } catch (IOException | IllegalArgumentException exception) {
            return UserSettings.defaults();
        }
    }

    public void save(UserSettings settings) {
        Properties properties = new Properties();
        properties.setProperty(LANGUAGE_KEY, settings.language().getCode());

        try {
            appDataPathService.createConfigDirectory();
            try (OutputStream outputStream = Files.newOutputStream(appDataPathService.getSettingsPath())) {
                properties.store(outputStream, "SIEM Lite user settings");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save user settings.", exception);
        }
    }
}
