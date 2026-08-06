package com.portfolio.siemlite.controller;

import com.portfolio.siemlite.config.AppDataPathService;
import com.portfolio.siemlite.config.SettingsService;
import com.portfolio.siemlite.config.UserSettings;
import com.portfolio.siemlite.localization.LanguageOption;
import com.portfolio.siemlite.localization.LocalizationService;
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
import com.portfolio.siemlite.windows.PowerShellWindowsEventLogCommandRunner;
import com.portfolio.siemlite.windows.WindowsEventLogImportResult;
import com.portfolio.siemlite.windows.WindowsEventLogImportService;
import com.portfolio.siemlite.windows.WindowsEventLogQuery;
import com.portfolio.siemlite.windows.WindowsEventLogWarningCode;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainController {

    static final long WINDOWS_REFRESH_INTERVAL_SECONDS = 120L;
    static final int WINDOWS_TOOLTIP_MAX_LENGTH = 2_000;

    private static final DateTimeFormatter WINDOWS_STATUS_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final AppDataPathService appDataPathService = new AppDataPathService();
    private final SettingsService settingsService = new SettingsService(appDataPathService);
    private final DatabaseInitializer databaseInitializer = new DatabaseInitializer();
    private final LogParser logParser = new GenericLogParser();
    private final DetectionService detectionService = new DetectionService();
    private final EventFilterService eventFilterService = new EventFilterService();
    private final ObservableList<LogEvent> allEvents = FXCollections.observableArrayList();
    private final ObservableList<LogEvent> windowsEvents = FXCollections.observableArrayList();
    private final ObservableList<SavedLogEvent> savedEvents = FXCollections.observableArrayList();
    private final WindowsEventLogImportService windowsEventLogImportService =
            new WindowsEventLogImportService(new PowerShellWindowsEventLogCommandRunner());
    private final Clock windowsRefreshClock = Clock.systemUTC();
    private final ScheduledExecutorService windowsRefreshExecutor =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "siem-lite-windows-event-refresh");
                thread.setDaemon(true);
                return thread;
            });
    private final AtomicBoolean windowsRefreshInProgress = new AtomicBoolean();
    private final AtomicBoolean windowsRefreshStopped = new AtomicBoolean();
    private SavedEventService savedEventService;
    private LocalizationService localizationService;
    private String saveWarning = "";
    private String loadWarning = "";
    private String windowsStatus = "";
    private boolean hasCompleteWindowsResult;
    private Instant lastCompleteWindowsUpdate;

    @FXML
    private TabPane mainTabPane;

    @FXML
    private Tab windowsEventsTab;

    @FXML
    private Tab importedEventsTab;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<SeverityFilterOption> severityComboBox;

    @FXML
    private ComboBox<LanguageOption> languageComboBox;

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
    private TableView<LogEvent> windowsEventsTable;

    @FXML
    private Label windowsPlaceholderLabel;

    @FXML
    private TableColumn<LogEvent, Integer> windowsLineNumberColumn;

    @FXML
    private TableColumn<LogEvent, String> windowsTimestampColumn;

    @FXML
    private TableColumn<LogEvent, Severity> windowsSeverityColumn;

    @FXML
    private TableColumn<LogEvent, String> windowsSourceColumn;

    @FXML
    private TableColumn<LogEvent, String> windowsSuspiciousColumn;

    @FXML
    private TableColumn<LogEvent, String> windowsKeywordColumn;

    @FXML
    private TableColumn<LogEvent, String> windowsMessageColumn;

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
        UserSettings userSettings = settingsService.load();
        localizationService = new LocalizationService(userSettings.language());
        configureTable();
        configureSavedEventsTable();
        configureLanguageSelector(userSettings.language());
        configureFilters();
        eventsTable.setItems(allEvents);
        windowsEventsTable.setItems(windowsEvents);
        savedEventsTable.setItems(savedEvents);
        mainTabPane.getSelectionModel().select(windowsEventsTab);
        mainTabPane.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldTab, newTab) -> {
                    if (newTab == windowsEventsTab && !windowsStatus.isBlank()) {
                        statusLabel.setText(windowsStatus);
                    }
                });
        initializePersistence();
        startWindowsEventAutoRefresh();
    }

    @FXML
    private void onImportLog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(localizationService.get("dialog.import.title"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(localizationService.get("dialog.import.extension"), "*.log", "*.txt"));

        File selectedFile = fileChooser.showOpenDialog(eventsTable.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        mainTabPane.getSelectionModel().select(importedEventsTab);

        try {
            saveWarning = "";
            loadWarning = "";
            List<LogEvent> parsedEvents = logParser.parse(selectedFile.toPath());
            detectionService.detectSuspiciousEvents(parsedEvents);
            allEvents.setAll(parsedEvents);
            applyFilters();
            SavedEventSaveResult saveResult = saveSuspiciousEvents(selectedFile.toPath(), parsedEvents);
            loadWarning = loadSavedEvents();
            updateStatus(selectedFile.getName(), parsedEvents.size(), saveResult);
        } catch (IOException exception) {
            allEvents.clear();
            eventsTable.getItems().clear();
            statusLabel.setText(localizationService.get("status.importError"));
        }
    }

    private void configureTable() {
        configureLogEventTable(
                lineNumberColumn,
                timestampColumn,
                severityColumn,
                sourceColumn,
                suspiciousColumn,
                keywordColumn,
                messageColumn);
        configureLogEventTable(
                windowsLineNumberColumn,
                windowsTimestampColumn,
                windowsSeverityColumn,
                windowsSourceColumn,
                windowsSuspiciousColumn,
                windowsKeywordColumn,
                windowsMessageColumn);
        configureWindowsMessageTooltip();
    }

    private void configureLogEventTable(
            TableColumn<LogEvent, Integer> lineNumber,
            TableColumn<LogEvent, String> timestamp,
            TableColumn<LogEvent, Severity> severity,
            TableColumn<LogEvent, String> source,
            TableColumn<LogEvent, String> suspicious,
            TableColumn<LogEvent, String> keyword,
            TableColumn<LogEvent, String> message) {
        lineNumber.setCellValueFactory(new PropertyValueFactory<>("lineNumber"));
        timestamp.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        severity.setCellValueFactory(new PropertyValueFactory<>("severity"));
        source.setCellValueFactory(new PropertyValueFactory<>("source"));
        suspicious.setCellValueFactory(cellData -> new SimpleStringProperty(
                localizationService.get(cellData.getValue().isSuspicious() ? "value.yes" : "value.no")));
        keyword.setCellValueFactory(new PropertyValueFactory<>("matchedKeyword"));
        message.setCellValueFactory(new PropertyValueFactory<>("message"));
    }

    private void configureWindowsMessageTooltip() {
        windowsMessageColumn.setCellFactory(column -> new TableCell<>() {
            private final Tooltip messageTooltip = createMessageTooltip();

            @Override
            protected void updateItem(String message, boolean empty) {
                super.updateItem(message, empty);
                if (empty || message == null || message.isBlank()) {
                    setText(null);
                    setTooltip(null);
                    return;
                }

                setText(message);
                messageTooltip.setText(limitWindowsTooltip(message));
                setTooltip(messageTooltip);
            }

            private Tooltip createMessageTooltip() {
                Tooltip tooltip = new Tooltip();
                tooltip.setWrapText(true);
                tooltip.setMaxWidth(700);
                return tooltip;
            }
        });
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
                new SeverityFilterOption(localizationService.get("filter.severity.all"), null),
                new SeverityFilterOption(Severity.INFO.name(), Severity.INFO),
                new SeverityFilterOption(Severity.WARN.name(), Severity.WARN),
                new SeverityFilterOption(Severity.ERROR.name(), Severity.ERROR),
                new SeverityFilterOption(Severity.CRITICAL.name(), Severity.CRITICAL),
                new SeverityFilterOption(Severity.UNKNOWN.name(), Severity.UNKNOWN)));
        severityComboBox.getSelectionModel().selectFirst();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        severityComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void configureLanguageSelector(LanguageOption selectedLanguage) {
        languageComboBox.setItems(FXCollections.observableArrayList(LanguageOption.supportedLanguages()));
        languageComboBox.getSelectionModel().select(selectedLanguage);
        languageComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue == oldValue) {
                return;
            }

            try {
                settingsService.save(new UserSettings(newValue));
                localizationService = new LocalizationService(newValue);
                statusLabel.setText(localizationService.get("language.restartRequired"));
            } catch (IllegalStateException exception) {
                statusLabel.setText(localizationService.get("language.saveError"));
            }
        });
    }

    private void startWindowsEventAutoRefresh() {
        windowsPlaceholderLabel.setText(localizationService.get("placeholder.windows.loading"));
        setWindowsStatus(localizationService.get("status.windows.loading"));
        windowsRefreshExecutor.scheduleWithFixedDelay(
                this::refreshWindowsEvents,
                0,
                WINDOWS_REFRESH_INTERVAL_SECONDS,
                TimeUnit.SECONDS);
    }

    private void refreshWindowsEvents() {
        if (windowsRefreshStopped.get() || !windowsRefreshInProgress.compareAndSet(false, true)) {
            return;
        }

        try {
            runOnFxThreadIfActive(() -> {
                if (windowsEvents.isEmpty()) {
                    windowsPlaceholderLabel.setText(
                            localizationService.get("placeholder.windows.loading"));
                }
            });
            WindowsEventLogQuery query = WindowsEventLogQuery.last24Hours(windowsRefreshClock);
            WindowsEventLogImportResult result = windowsEventLogImportService.importEvents(query);
            List<LogEvent> refreshedEvents = result.events().stream()
                    .map(importedEvent -> importedEvent.logEvent())
                    .toList();
            Instant completedAt = windowsRefreshClock.instant();
            runOnFxThreadIfActive(() ->
                    applyWindowsRefreshResult(result, refreshedEvents, completedAt));
        } catch (RuntimeException exception) {
            runOnFxThreadIfActive(() -> {
                windowsPlaceholderLabel.setText(
                        localizationService.get("placeholder.windows.partialOrUnavailable"));
                setWindowsStatus(localizationService.get("status.windows.loadFailed"));
            });
        } finally {
            windowsRefreshInProgress.set(false);
        }
    }

    private void applyWindowsRefreshResult(
            WindowsEventLogImportResult result,
            List<LogEvent> refreshedEvents,
            Instant completedAt) {
        windowsPlaceholderLabel.setText(localizationService.get(windowsPlaceholderKey(result)));

        if (result.metadataComplete()) {
            windowsEvents.setAll(refreshedEvents);
            hasCompleteWindowsResult = true;
            lastCompleteWindowsUpdate = completedAt;
            setWindowsStatus(buildWindowsStatus(
                    result,
                    formatWindowsUpdateTime(completedAt),
                    localizationService));
            return;
        }

        if (!hasCompleteWindowsResult) {
            windowsEvents.setAll(refreshedEvents);
            setWindowsStatus(buildWindowsStatus(
                    result,
                    formatWindowsUpdateTime(completedAt),
                    localizationService));
            return;
        }

        setWindowsStatus(buildWindowsRetainedStatus(
                windowsEvents.size(),
                result,
                formatWindowsUpdateTime(lastCompleteWindowsUpdate),
                localizationService));
    }

    private void setWindowsStatus(String status) {
        windowsStatus = status == null ? "" : status;
        if (mainTabPane.getSelectionModel().getSelectedItem() == windowsEventsTab) {
            statusLabel.setText(windowsStatus);
        }
    }

    private void runOnFxThreadIfActive(Runnable action) {
        if (windowsRefreshStopped.get()) {
            return;
        }

        try {
            Platform.runLater(() -> {
                if (!windowsRefreshStopped.get()) {
                    action.run();
                }
            });
        } catch (IllegalStateException ignored) {
            // The JavaFX runtime is already stopping.
        }
    }

    private String formatWindowsUpdateTime(Instant updatedAt) {
        return updatedAt == null ? "" : WINDOWS_STATUS_TIME_FORMAT.format(updatedAt);
    }

    private void applyFilters() {
        String searchText = searchField.getText();
        SeverityFilterOption selectedSeverity = severityComboBox.getValue();
        String severityValue = selectedSeverity == null || selectedSeverity.severity() == null
                ? "All"
                : selectedSeverity.severity().name();
        List<LogEvent> filteredEvents = eventFilterService.filter(allEvents, searchText, severityValue);
        eventsTable.setItems(FXCollections.observableArrayList(filteredEvents));
        statusLabel.setText(localizationService.format("status.visibleEvents", filteredEvents.size(), allEvents.size()));
    }

    private void initializePersistence() {
        try {
            appDataPathService.createDataDirectory();
            Path databasePath = appDataPathService.getDatabasePath();
            databaseInitializer.initialize(databasePath);
            savedEventService = new SavedEventService(new SavedEventRepository(databasePath));
            String warning = loadSavedEvents();
            if (!warning.isBlank()) {
                statusLabel.setText(warning);
            }
        } catch (IOException | DatabaseException exception) {
            savedEventService = null;
            statusLabel.setText(localizationService.get("status.persistenceUnavailable"));
        }
    }

    private SavedEventSaveResult saveSuspiciousEvents(Path importedFilePath, List<LogEvent> parsedEvents) {
        if (savedEventService == null) {
            return new SavedEventSaveResult(0, 0, 0);
        }

        try {
            return savedEventService.saveSuspiciousEvents(parsedEvents, importedFilePath);
        } catch (DatabaseException exception) {
            saveWarning = localizationService.get("warning.saveSuspiciousEvents");
            return new SavedEventSaveResult(0, 0, 0);
        }
    }

    private String loadSavedEvents() {
        if (savedEventService == null) {
            savedEvents.clear();
            return "";
        }

        try {
            savedEvents.setAll(savedEventService.loadSavedEvents());
            return "";
        } catch (DatabaseException exception) {
            savedEvents.clear();
            return localizationService.get("warning.loadSavedEvents");
        }
    }

    private void updateStatus(String fileName, int eventCount, SavedEventSaveResult saveResult) {
        long suspiciousCount = allEvents.stream().filter(LogEvent::isSuspicious).count();
        statusLabel.setText(buildImportStatus(
                fileName,
                eventCount,
                suspiciousCount,
                savedEventService != null,
                saveResult,
                saveWarning,
                loadWarning,
                localizationService));
    }

    static String buildImportStatus(
            String fileName,
            int eventCount,
            long suspiciousCount,
            boolean autoSaveAvailable,
            SavedEventSaveResult saveResult,
            String saveWarning,
            String loadWarning,
            LocalizationService localizationService) {
        String status = localizationService.format(
                "status.importedFile",
                fileName,
                eventCount,
                suspiciousCount);

        if (saveWarning != null && !saveWarning.isBlank()) {
            status += " | " + localizationService.format("status.saveWarning", saveWarning);
        } else if (!autoSaveAvailable) {
            status += " | " + localizationService.get("status.autoSaveUnavailable");
        } else {
            status += " | " + localizationService.format(
                    "status.savedSummary",
                    saveResult.savedEvents(),
                    saveResult.duplicateEvents());
        }

        if (loadWarning != null && !loadWarning.isBlank()) {
            status += " | " + localizationService.format("status.loadWarning", loadWarning);
        }

        return status;
    }

    static String buildWindowsStatus(
            WindowsEventLogImportResult result,
            String lastUpdated,
            LocalizationService localizationService) {
        if (result.warnings().contains(WindowsEventLogWarningCode.UNSUPPORTED_PLATFORM)) {
            return localizationService.get("status.windows.unsupportedPlatform");
        }

        String status;
        if (isDefinitiveEmptyWindowsResult(result)) {
            status = localizationService.get("status.windows.noEvents");
        } else if (result.metadataComplete() && result.totalEvents() > 0) {
            status = localizationService.format(
                    "status.windows.loadedComplete",
                    result.totalEvents(),
                    result.logsWithEvents(),
                    result.suspiciousEvents());
        } else {
            status = localizationService.format(
                    "status.windows.loadedPartial",
                    result.totalEvents(),
                    result.suspiciousEvents());
        }

        return appendWindowsStatusDetails(status, result, lastUpdated, localizationService);
    }

    static String windowsPlaceholderKey(WindowsEventLogImportResult result) {
        return isDefinitiveEmptyWindowsResult(result)
                ? "placeholder.windows.noEvents"
                : "placeholder.windows.partialOrUnavailable";
    }

    private static boolean isDefinitiveEmptyWindowsResult(WindowsEventLogImportResult result) {
        return result.totalEvents() == 0
                && result.metadataComplete()
                && result.warnings().isEmpty()
                && !result.timedOut()
                && !result.reachedCap();
    }

    static String limitWindowsTooltip(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        if (message.length() <= WINDOWS_TOOLTIP_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, WINDOWS_TOOLTIP_MAX_LENGTH - 1) + "…";
    }

    static String buildWindowsRetainedStatus(
            int retainedEventCount,
            WindowsEventLogImportResult refreshResult,
            String lastUpdated,
            LocalizationService localizationService) {
        String status = localizationService.format(
                "status.windows.partialRetained",
                retainedEventCount);
        return appendWindowsStatusDetails(
                status,
                refreshResult,
                lastUpdated,
                localizationService);
    }

    private static String appendWindowsStatusDetails(
            String status,
            WindowsEventLogImportResult result,
            String lastUpdated,
            LocalizationService localizationService) {
        if (lastUpdated != null && !lastUpdated.isBlank()) {
            status += " | " + localizationService.format(
                    "status.windows.lastUpdated",
                    lastUpdated);
        }
        if (result.timedOut()) {
            status += " | " + localizationService.get("status.windows.timeout");
        }
        if (result.reachedCap()) {
            status += " | " + localizationService.get("status.windows.capReached");
        }
        if (!result.warnings().isEmpty()) {
            status += " | " + localizationService.format(
                    "status.windows.warningCount",
                    result.warnings().size());
        }
        return status;
    }

    public void shutdown() {
        if (windowsRefreshStopped.compareAndSet(false, true)) {
            windowsRefreshExecutor.shutdownNow();
        }
    }

    private record SeverityFilterOption(String label, Severity severity) {

        @Override
        public String toString() {
            return label;
        }
    }
}
