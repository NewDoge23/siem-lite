package com.portfolio.siemlite.controller;

import com.portfolio.siemlite.config.AppDataPathService;
import com.portfolio.siemlite.model.LogEvent;
import com.portfolio.siemlite.model.SavedLogEvent;
import com.portfolio.siemlite.model.Severity;
import com.portfolio.siemlite.parser.GenericLogParser;
import com.portfolio.siemlite.parser.LogParser;
import com.portfolio.siemlite.repository.DatabaseException;
import com.portfolio.siemlite.repository.DatabaseInitializer;
import com.portfolio.siemlite.repository.SavedEventRepository;
import com.portfolio.siemlite.service.DetectionService;
import com.portfolio.siemlite.service.EventFilterService;
import com.portfolio.siemlite.service.SavedEventSaveResult;
import com.portfolio.siemlite.service.SavedEventService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class MainController {

    private final AppDataPathService appDataPathService = new AppDataPathService();
    private final DatabaseInitializer databaseInitializer = new DatabaseInitializer();
    private final LogParser logParser = new GenericLogParser();
    private final DetectionService detectionService = new DetectionService();
    private final EventFilterService eventFilterService = new EventFilterService();
    private final ObservableList<LogEvent> allEvents = FXCollections.observableArrayList();
    private final ObservableList<SavedLogEvent> savedEvents = FXCollections.observableArrayList();
    private SavedEventService savedEventService;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> severityComboBox;

    @FXML
    private TableView<LogEvent> eventsTable;

    @FXML
    private TableColumn<LogEvent, Integer> lineNumberColumn;

    @FXML
    private TableColumn<LogEvent, String> timestampColumn;

    @FXML
    private TableColumn<LogEvent, Severity> severityColumn;

    @FXML
    private TableColumn<LogEvent, String> sourceColumn;

    @FXML
    private TableColumn<LogEvent, String> suspiciousColumn;

    @FXML
    private TableColumn<LogEvent, String> keywordColumn;

    @FXML
    private TableColumn<LogEvent, String> messageColumn;

    @FXML
    private TableView<SavedLogEvent> savedEventsTable;

    @FXML
    private TableColumn<SavedLogEvent, String> savedAtColumn;

    @FXML
    private TableColumn<SavedLogEvent, String> savedFileColumn;

    @FXML
    private TableColumn<SavedLogEvent, Integer> savedLineNumberColumn;

    @FXML
    private TableColumn<SavedLogEvent, String> savedTimestampColumn;

    @FXML
    private TableColumn<SavedLogEvent, Severity> savedSeverityColumn;

    @FXML
    private TableColumn<SavedLogEvent, String> savedSourceColumn;

    @FXML
    private TableColumn<SavedLogEvent, String> savedKeywordColumn;

    @FXML
    private TableColumn<SavedLogEvent, String> savedMessageColumn;

    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        configureTable();
        configureSavedEventsTable();
        configureFilters();
        eventsTable.setItems(allEvents);
        savedEventsTable.setItems(savedEvents);
        initializePersistence();
    }

    @FXML
    private void onImportLog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Log File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Log files", "*.log", "*.txt"));

        File selectedFile = fileChooser.showOpenDialog(eventsTable.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        try {
            List<LogEvent> parsedEvents = logParser.parse(selectedFile.toPath());
            detectionService.detectSuspiciousEvents(parsedEvents);
            allEvents.setAll(parsedEvents);
            applyFilters();
            SavedEventSaveResult saveResult = saveSuspiciousEvents(selectedFile.toPath(), parsedEvents);
            loadSavedEvents();
            updateStatus(selectedFile.getName(), parsedEvents.size(), saveResult);
        } catch (IOException exception) {
            allEvents.clear();
            eventsTable.getItems().clear();
            statusLabel.setText("Could not import file: " + exception.getMessage());
        }
    }

    private void configureTable() {
        lineNumberColumn.setCellValueFactory(new PropertyValueFactory<>("lineNumber"));
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        severityColumn.setCellValueFactory(new PropertyValueFactory<>("severity"));
        sourceColumn.setCellValueFactory(new PropertyValueFactory<>("source"));
        suspiciousColumn.setCellValueFactory(new PropertyValueFactory<>("suspiciousLabel"));
        keywordColumn.setCellValueFactory(new PropertyValueFactory<>("matchedKeyword"));
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
    }

    private void configureSavedEventsTable() {
        savedAtColumn.setCellValueFactory(new PropertyValueFactory<>("savedAt"));
        savedFileColumn.setCellValueFactory(new PropertyValueFactory<>("importedFileName"));
        savedLineNumberColumn.setCellValueFactory(new PropertyValueFactory<>("lineNumber"));
        savedTimestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        savedSeverityColumn.setCellValueFactory(new PropertyValueFactory<>("severity"));
        savedSourceColumn.setCellValueFactory(new PropertyValueFactory<>("source"));
        savedKeywordColumn.setCellValueFactory(new PropertyValueFactory<>("matchedKeyword"));
        savedMessageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
    }

    private void configureFilters() {
        severityComboBox.setItems(FXCollections.observableArrayList(
                "All", Severity.INFO.name(), Severity.WARN.name(), Severity.ERROR.name(),
                Severity.CRITICAL.name(), Severity.UNKNOWN.name()));
        severityComboBox.getSelectionModel().selectFirst();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        severityComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void applyFilters() {
        String searchText = searchField.getText();
        String selectedSeverity = severityComboBox.getValue();
        List<LogEvent> filteredEvents = eventFilterService.filter(allEvents, searchText, selectedSeverity);
        eventsTable.setItems(FXCollections.observableArrayList(filteredEvents));
        statusLabel.setText("Visible events: " + filteredEvents.size() + " / " + allEvents.size());
    }

    private void initializePersistence() {
        try {
            appDataPathService.createDataDirectory();
            Path databasePath = appDataPathService.getDatabasePath();
            databaseInitializer.initialize(databasePath);
            savedEventService = new SavedEventService(new SavedEventRepository(databasePath));
            loadSavedEvents();
        } catch (IOException | DatabaseException exception) {
            savedEventService = null;
            statusLabel.setText("Local persistence unavailable: " + exception.getMessage());
        }
    }

    private SavedEventSaveResult saveSuspiciousEvents(Path importedFilePath, List<LogEvent> parsedEvents) {
        if (savedEventService == null) {
            return new SavedEventSaveResult(0, 0, 0);
        }

        try {
            return savedEventService.saveSuspiciousEvents(parsedEvents, importedFilePath);
        } catch (DatabaseException exception) {
            statusLabel.setText("Could not save suspicious events: " + exception.getMessage());
            return new SavedEventSaveResult(0, 0, 0);
        }
    }

    private void loadSavedEvents() {
        if (savedEventService == null) {
            savedEvents.clear();
            return;
        }

        try {
            savedEvents.setAll(savedEventService.loadSavedEvents());
        } catch (DatabaseException exception) {
            savedEvents.clear();
            statusLabel.setText("Could not load saved events: " + exception.getMessage());
        }
    }

    private void updateStatus(String fileName, int eventCount, SavedEventSaveResult saveResult) {
        long suspiciousCount = allEvents.stream().filter(LogEvent::isSuspicious).count();
        String status = "Imported file: " + fileName
                + " | Events: " + eventCount
                + " | Suspicious: " + suspiciousCount;

        if (savedEventService == null) {
            status += " | Auto-save unavailable";
        } else {
            status += " | Saved: " + saveResult.savedEvents()
                    + " | Duplicates skipped: " + saveResult.duplicateEvents();
        }

        statusLabel.setText(status);
    }
}
