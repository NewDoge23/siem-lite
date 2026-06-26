package com.portfolio.siemlite.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AppDataPathService {

    private static final String APP_DIRECTORY = "SIEM Lite";
    private static final String DATA_DIRECTORY = "data";
    private static final String CONFIG_DIRECTORY = "config";
    private static final String DATABASE_FILE = "siem-lite.db";
    private static final String SETTINGS_FILE = "settings.properties";

    private final Path appDataRoot;

    public AppDataPathService() {
        this(resolveDefaultAppDataRoot());
    }

    public AppDataPathService(Path appDataRoot) {
        this.appDataRoot = appDataRoot;
    }

    public Path getDatabasePath() {
        return appDataRoot.resolve(APP_DIRECTORY).resolve(DATA_DIRECTORY).resolve(DATABASE_FILE);
    }

    public Path getSettingsPath() {
        return appDataRoot.resolve(APP_DIRECTORY).resolve(CONFIG_DIRECTORY).resolve(SETTINGS_FILE);
    }

    public Path createDataDirectory() throws IOException {
        return Files.createDirectories(getDatabasePath().getParent());
    }

    public Path createConfigDirectory() throws IOException {
        return Files.createDirectories(getSettingsPath().getParent());
    }

    private static Path resolveDefaultAppDataRoot() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData);
        }

        return Path.of(System.getProperty("user.home"), "AppData", "Roaming");
    }
}
