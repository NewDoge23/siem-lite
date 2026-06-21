package com.portfolio.siemlite.service;

import com.portfolio.siemlite.model.LogEvent;
import com.portfolio.siemlite.model.Severity;
import com.portfolio.siemlite.repository.DatabaseInitializer;
import com.portfolio.siemlite.repository.SavedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SavedEventServiceTest {

    @TempDir
    Path tempDir;

    private SavedEventRepository repository;
    private SavedEventService service;

    @BeforeEach
    void setUp() {
        Path databasePath = tempDir.resolve("test.db");
        new DatabaseInitializer().initialize(databasePath);
        repository = new SavedEventRepository(databasePath);
        service = new SavedEventService(repository);
    }

    @Test
    void savesOnlySuspiciousEvents() {
        LogEvent suspicious = event(1, "Failed login detected", true);
        suspicious.setMatchedKeyword("failed");
        LogEvent normal = event(2, "Normal service started", false);

        SavedEventSaveResult result = service.saveSuspiciousEvents(
                List.of(suspicious, normal),
                tempDir.resolve("sample-system.log"));

        assertEquals(1, result.suspiciousEvents());
        assertEquals(1, result.savedEvents());
        assertEquals(0, result.duplicateEvents());
        assertEquals(1, repository.count());
    }

    @Test
    void reportsDuplicatesWhenSavingSameSuspiciousEventAgain() {
        LogEvent suspicious = event(1, "Failed login detected", true);
        suspicious.setMatchedKeyword("failed");
        Path importedFile = tempDir.resolve("sample-system.log");

        service.saveSuspiciousEvents(List.of(suspicious), importedFile);
        SavedEventSaveResult secondResult = service.saveSuspiciousEvents(List.of(suspicious), importedFile);

        assertEquals(1, secondResult.suspiciousEvents());
        assertEquals(0, secondResult.savedEvents());
        assertEquals(1, secondResult.duplicateEvents());
        assertEquals(1, repository.count());
    }

    private LogEvent event(int lineNumber, String message, boolean suspicious) {
        LogEvent event = new LogEvent(
                lineNumber,
                "2026-06-18 09:30:00",
                Severity.ERROR,
                "auth",
                message,
                "2026-06-18 09:30:00 ERROR [auth] " + message);
        event.setSuspicious(suspicious);
        return event;
    }
}
