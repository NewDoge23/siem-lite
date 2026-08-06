package com.portfolio.siemlite.controller;

import com.portfolio.siemlite.localization.LanguageOption;
import com.portfolio.siemlite.localization.LocalizationService;
import com.portfolio.siemlite.model.LogEvent;
import com.portfolio.siemlite.model.Severity;
import com.portfolio.siemlite.service.SavedEventSaveResult;
import com.portfolio.siemlite.windows.WindowsEventLogEntry;
import com.portfolio.siemlite.windows.WindowsEventLogIdentity;
import com.portfolio.siemlite.windows.WindowsEventLogImportResult;
import com.portfolio.siemlite.windows.WindowsEventLogImportedEvent;
import com.portfolio.siemlite.windows.WindowsEventLogWarningCode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainControllerStatusTest {

    private final LocalizationService localizationService = new LocalizationService(LanguageOption.ENGLISH);

    @Test
    void includesSaveSummaryWhenPersistenceSucceeds() {
        String status = MainController.buildImportStatus(
                "sample-system.log",
                13,
                7,
                true,
                new SavedEventSaveResult(7, 5, 2),
                "",
                "",
                localizationService);

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
                "",
                localizationService);

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
                "Could not load saved events",
                localizationService);

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
                "Could not load saved events",
                localizationService);

        assertEquals(
                "Imported file: sample-system.log | Events: 13 | Suspicious: 7 | Save warning: Could not save suspicious events | Load warning: Could not load saved events",
                status);
    }

    @Test
    void buildsCompleteWindowsStatusWithReliableLogCount() {
        WindowsEventLogImportResult result = windowsResult(
                3,
                1,
                2,
                true,
                List.of(),
                false,
                false);

        String status = MainController.buildWindowsStatus(
                result,
                "14:05:00",
                localizationService);

        assertEquals(
                "Loaded 3 Windows events from 2 logs | Suspicious: 1 | Last updated: 14:05:00",
                status);
    }

    @Test
    void buildsNoEventsStatusForCompleteEmptyResult() {
        WindowsEventLogImportResult result = windowsResult(
                0,
                0,
                0,
                true,
                List.of(),
                false,
                false);

        String status = MainController.buildWindowsStatus(
                result,
                "14:05:30",
                localizationService);

        assertEquals(
                "No Windows events were found in the last 24 hours. | Last updated: 14:05:30",
                status);
    }

    @Test
    void distinguishesDefinitiveNoEventsFromPartialOrFailedEmptyResults() {
        WindowsEventLogImportResult definitiveEmpty = windowsResult(
                0, 0, 0, true, List.of(), false, false);
        WindowsEventLogImportResult incomplete = windowsResult(
                0,
                0,
                0,
                false,
                List.of(WindowsEventLogWarningCode.SUMMARY_MISSING),
                false,
                false);
        WindowsEventLogImportResult warning = windowsResult(
                0,
                0,
                0,
                true,
                List.of(WindowsEventLogWarningCode.INVALID_NDJSON),
                false,
                false);
        WindowsEventLogImportResult timedOut = windowsResult(
                0, 0, 0, true, List.of(), false, true);
        WindowsEventLogImportResult capped = windowsResult(
                0, 0, 0, true, List.of(), true, false);

        assertEquals(
                "placeholder.windows.noEvents",
                MainController.windowsPlaceholderKey(definitiveEmpty));
        assertEquals(
                "placeholder.windows.partialOrUnavailable",
                MainController.windowsPlaceholderKey(incomplete));
        assertEquals(
                "placeholder.windows.partialOrUnavailable",
                MainController.windowsPlaceholderKey(warning));
        assertEquals(
                "placeholder.windows.partialOrUnavailable",
                MainController.windowsPlaceholderKey(timedOut));
        assertEquals(
                "placeholder.windows.partialOrUnavailable",
                MainController.windowsPlaceholderKey(capped));

        String status = MainController.buildWindowsStatus(
                incomplete,
                "14:05:45",
                localizationService);

        assertTrue(status.startsWith(
                "Loaded 0 Windows events | Suspicious: 0 | Partial result"));
        assertTrue(status.contains("Load warnings: 1"));
        assertFalse(status.contains("No Windows events were found"));
        assertFalse(status.contains("SUMMARY_MISSING"));
    }

    @Test
    void buildsPartialWindowsStatusWithoutUnreliableLogCount() {
        WindowsEventLogImportResult result = windowsResult(
                2,
                1,
                0,
                false,
                List.of(
                        WindowsEventLogWarningCode.SUMMARY_MISSING,
                        WindowsEventLogWarningCode.TIMEOUT,
                        WindowsEventLogWarningCode.STDOUT_CAP_REACHED),
                true,
                true);

        String status = MainController.buildWindowsStatus(
                result,
                "14:06:00",
                localizationService);

        assertEquals(
                "Loaded 2 Windows events | Suspicious: 1 | Partial result"
                        + " | Last updated: 14:06:00"
                        + " | Timed out while reading Windows Event Logs. Partial results may be shown."
                        + " | Safety limit reached. Some events may not be shown."
                        + " | Load warnings: 3",
                status);
        assertFalse(status.contains("from 0 logs"));
        assertFalse(status.contains("SUMMARY_MISSING"));
        assertFalse(status.contains("STDOUT_CAP_REACHED"));
    }

    @Test
    void buildsLocalizedUnsupportedPlatformStatusWithoutRawWarningCode() {
        WindowsEventLogImportResult result = windowsResult(
                0,
                0,
                0,
                false,
                List.of(WindowsEventLogWarningCode.UNSUPPORTED_PLATFORM),
                false,
                false);

        String status = MainController.buildWindowsStatus(
                result,
                "14:07:00",
                localizationService);

        assertEquals("Windows Event Logs are only available on Windows.", status);
        assertFalse(status.contains("UNSUPPORTED_PLATFORM"));
    }

    @Test
    void reportsPartialRefreshWhileKeepingPreviousTableData() {
        WindowsEventLogImportResult result = windowsResult(
                1,
                1,
                0,
                false,
                List.of(
                        WindowsEventLogWarningCode.SUMMARY_MISSING,
                        WindowsEventLogWarningCode.TIMEOUT),
                false,
                true);

        String status = MainController.buildWindowsRetainedStatus(
                25,
                result,
                "13:59:00",
                localizationService);

        assertEquals(
                "Partial refresh received. Keeping 25 previously loaded Windows events."
                        + " | Last updated: 13:59:00"
                        + " | Timed out while reading Windows Event Logs. Partial results may be shown."
                        + " | Load warnings: 2",
                status);
        assertFalse(status.contains("SUMMARY_MISSING"));
        assertFalse(status.contains("TIMEOUT"));
    }

    @Test
    void usesTwoMinuteWindowsRefreshInterval() {
        assertEquals(120L, MainController.WINDOWS_REFRESH_INTERVAL_SECONDS);
    }

    @Test
    void boundsWindowsMessageTooltipWithoutChangingShortMessages() {
        String shortMessage = "PowerShell event details";
        String longMessage = "x".repeat(MainController.WINDOWS_TOOLTIP_MAX_LENGTH + 100);

        assertEquals(shortMessage, MainController.limitWindowsTooltip(shortMessage));
        assertEquals("", MainController.limitWindowsTooltip(null));

        String limited = MainController.limitWindowsTooltip(longMessage);
        assertEquals(MainController.WINDOWS_TOOLTIP_MAX_LENGTH, limited.length());
        assertTrue(limited.endsWith("…"));
    }

    @Test
    void buildsSpanishWindowsStatusFromTheSameStructuredResult() {
        LocalizationService spanish = new LocalizationService(LanguageOption.SPANISH);
        WindowsEventLogImportResult result = windowsResult(
                1,
                0,
                1,
                true,
                List.of(WindowsEventLogWarningCode.LOG_SKIPPED),
                false,
                false);

        String status = MainController.buildWindowsStatus(result, "14:08:00", spanish);

        assertTrue(status.startsWith("Se cargaron 1 eventos de Windows desde 1 logs"));
        assertTrue(status.contains("Última actualización: 14:08:00"));
        assertTrue(status.contains("Advertencias de carga: 1"));
        assertFalse(status.contains("LOG_SKIPPED"));
    }

    private WindowsEventLogImportResult windowsResult(
            int totalEvents,
            int suspiciousEvents,
            int logsWithEvents,
            boolean metadataComplete,
            List<WindowsEventLogWarningCode> warnings,
            boolean reachedCap,
            boolean timedOut) {
        List<WindowsEventLogImportedEvent> events = new ArrayList<>();
        for (int index = 0; index < totalEvents; index++) {
            WindowsEventLogEntry entry = new WindowsEventLogEntry(
                    "2026-08-06T12:00:00Z",
                    4,
                    "System",
                    "Provider",
                    100 + index,
                    1_000L + index,
                    "HOST",
                    "Message " + index,
                    null);
            LogEvent logEvent = new LogEvent(
                    index + 1,
                    "2026-08-06 12:00:00",
                    Severity.INFO,
                    "Provider",
                    entry.message(),
                    entry.message());
            if (index < suspiciousEvents) {
                logEvent.setSuspicious(true);
                logEvent.setMatchedKeyword("failed");
            }
            events.add(new WindowsEventLogImportedEvent(
                    entry,
                    logEvent,
                    WindowsEventLogIdentity.from(entry)));
        }

        return new WindowsEventLogImportResult(
                events,
                totalEvents,
                suspiciousEvents,
                logsWithEvents,
                logsWithEvents,
                0,
                metadataComplete,
                warnings,
                reachedCap,
                timedOut);
    }
}
