package com.portfolio.siemlite.controller;

import com.portfolio.siemlite.model.LogEvent;
import com.portfolio.siemlite.model.Severity;
import com.portfolio.siemlite.parser.GenericLogParser;
import com.portfolio.siemlite.parser.LogParser;
import com.portfolio.siemlite.service.DetectionService;
import com.portfolio.siemlite.service.EventFilterService;
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
import java.util.List;

public class MainController {

    private final LogParser logParser = new GenericLogParser();
    private final DetectionService detectionService = new DetectionService();
    private final EventFilterService eventFilterService = new EventFilterService();
    private final ObservableList<LogEvent> allEvents = FXCollections.observableArrayList();

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
    private Label statusLabel;

    @FXML
    private void initialize() {
        configureTable();
        configureFilters();
        eventsTable.setItems(allEvents);
    }

    @FXML
    private void onImportLog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importar archivo de log");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos de log", "*.log", "*.txt"));

        File selectedFile = fileChooser.showOpenDialog(eventsTable.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        try {
            List<LogEvent> parsedEvents = logParser.parse(selectedFile.toPath());
            detectionService.detectSuspiciousEvents(parsedEvents);
            allEvents.setAll(parsedEvents);
            applyFilters();
            updateStatus(selectedFile.getName(), parsedEvents.size());
        } catch (IOException exception) {
            allEvents.clear();
            eventsTable.getItems().clear();
            statusLabel.setText("No se pudo importar el archivo: " + exception.getMessage());
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

    private void configureFilters() {
        severityComboBox.setItems(FXCollections.observableArrayList(
                "Todas", Severity.INFO.name(), Severity.WARN.name(), Severity.ERROR.name(),
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
        statusLabel.setText("Eventos visibles: " + filteredEvents.size() + " / " + allEvents.size());
    }

    private void updateStatus(String fileName, int eventCount) {
        long suspiciousCount = allEvents.stream().filter(LogEvent::isSuspicious).count();
        statusLabel.setText("Archivo importado: " + fileName
                + " | Eventos: " + eventCount
                + " | Sospechosos: " + suspiciousCount);
    }
}
