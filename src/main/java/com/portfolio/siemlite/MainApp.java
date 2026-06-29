package com.portfolio.siemlite;

import com.portfolio.siemlite.config.AppDataPathService;
import com.portfolio.siemlite.config.SettingsService;
import com.portfolio.siemlite.config.UserSettings;
import com.portfolio.siemlite.localization.LocalizationService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        AppDataPathService appDataPathService = new AppDataPathService();
        SettingsService settingsService = new SettingsService(appDataPathService);
        UserSettings userSettings = settingsService.load();
        LocalizationService localizationService = new LocalizationService(userSettings.language());

        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/fxml/main-view.fxml"),
                localizationService.getBundle());
        Scene scene = new Scene(loader.load(), 1100, 700);
        scene.getStylesheets().add(Objects.requireNonNull(
                MainApp.class.getResource("/css/styles.css")).toExternalForm());

        stage.setTitle(localizationService.get("app.title"));
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
