package com.portfolio.siemlite.controller;

import com.portfolio.siemlite.service.SavedEventSaveResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainControllerStatusTest {

    @Test
    void includesSaveSummaryWhenPersistenceSucceeds() {
        String status = MainController.buildImportStatus(
                "sample-system.log",
                13,
                7,
                true,
                new SavedEventSaveResult(7, 5, 2),
                "",
                "");

        assertEquals(
                "Imported file: sample-system.log | Events: 13 | Suspicious: 7 | Saved: 5 | Duplicates skipped: 2",
                status);
    }

    @Test
    void preservesSaveWarningWhenPersistenceFails() {
        String status = MainController.buildImportStatus(
                "sample-system.log",
                13,
                7,
                true,
                new SavedEventSaveResult(0, 0, 0),
                "Could not save suspicious events",
                "");

        assertEquals(
                "Imported file: sample-system.log | Events: 13 | Suspicious: 7 | Save warning: Could not save suspicious events",
                status);
    }

    @Test
    void preservesLoadWarningWhenSavedEventsRefreshFails() {
        String status = MainController.buildImportStatus(
                "sample-system.log",
                13,
                7,
                true,
                new SavedEventSaveResult(7, 7, 0),
                "",
                "Could not load saved events");

        assertEquals(
                "Imported file: sample-system.log | Events: 13 | Suspicious: 7 | Saved: 7 | Duplicates skipped: 0 | Load warning: Could not load saved events",
                status);
    }

    @Test
    void preservesSaveAndLoadWarningsTogether() {
        String status = MainController.buildImportStatus(
                "sample-system.log",
                13,
                7,
                true,
                new SavedEventSaveResult(0, 0, 0),
                "Could not save suspicious events",
                "Could not load saved events");

        assertEquals(
                "Imported file: sample-system.log | Events: 13 | Suspicious: 7 | Save warning: Could not save suspicious events | Load warning: Could not load saved events",
                status);
    }
}
