package com.portfolio.siemlite.repository;

import com.portfolio.siemlite.model.SavedLogEvent;
import com.portfolio.siemlite.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SavedEventRepositoryTest {

    @TempDir
    Path tempDir;

    private SavedEventRepository repository;

    @BeforeEach
    void setUp() {
        Path databasePath = tempDir.resolve("test.db");
        new DatabaseInitializer().initialize(databasePath);
        repository = new SavedEventRepository(databasePath);
    }

    @Test
    void savesAndLoadsSavedEvents() {
        SavedLogEvent event = event("hash-1", "Failed logon");

        boolean saved = repository.save(event);
        List<SavedLogEvent> savedEvents = repository.findAll();

        assertTrue(saved);
        assertEquals(1, savedEvents.size());
        assertEquals("Failed logon", savedEvents.getFirst().getMessage());
        assertEquals(Severity.ERROR, savedEvents.getFirst().getSeverity());
    }

    @Test
    void skipsDuplicateContentHash() {
        SavedLogEvent event = event("same-hash", "Failed logon");

        assertTrue(repository.save(event));
        assertFalse(repository.save(event));

        assertEquals(1, repository.count());
    }

    private SavedLogEvent event(String contentHash, String message) {
        return new SavedLogEvent(
                0,
                7,
                "2026-06-18 09:30:00",
                Severity.ERROR,
                "auth",
                message,
                "2026-06-18 09:30:00 ERROR [auth] Failed logon",
                true,
                "failed",
                "sample-system.log",
                tempDir.resolve("sample-system.log").toString(),
                "2026-06-18T09:31:00",
                contentHash);
    }
}
