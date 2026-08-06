package com.portfolio.siemlite.windows;

import com.portfolio.siemlite.model.Severity;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowsEventLogImportServiceTest {

    @Test
    void combinesRunnerMapperDetectionAndIdentityForPartialResults() {
        WindowsEventLogEntry invalidFirst = entry(
                "invalid-timestamp", 4, "Invalid first", 30L);
        WindowsEventLogEntry older = entry(
                "2026-08-06T10:00:00Z", 3, "Normal warning", 10L);
        WindowsEventLogEntry missingSecond = entry(
                null, 4, "Missing timestamp", 40L);
        WindowsEventLogEntry newerSuspicious = entry(
                "2026-08-06T12:00:00Z", 2, "Failed login detected", 20L);
        WindowsEventLogQuery query = WindowsEventLogQuery.last24Hours(
                Clock.fixed(Instant.parse("2026-08-06T13:00:00Z"), ZoneOffset.UTC));
        AtomicReference<WindowsEventLogQuery> receivedQuery = new AtomicReference<>();
        WindowsEventLogCommandRunner fakeRunner = requestedQuery -> {
            receivedQuery.set(requestedQuery);
            return new WindowsEventLogCommandResult(
                    List.of(invalidFirst, older, missingSecond, newerSuspicious),
                    3,
                    2,
                    1,
                    true,
                    List.of(WindowsEventLogWarningCode.LOG_SKIPPED),
                    true,
                    true);
        };
        WindowsEventLogImportService service = new WindowsEventLogImportService(fakeRunner);

        WindowsEventLogImportResult result = service.importEvents(query);

        assertSame(query, receivedQuery.get());
        assertEquals(4, result.totalEvents());
        assertEquals(1, result.suspiciousEvents());
        assertEquals(3, result.logsConsulted());
        assertEquals(2, result.logsWithEvents());
        assertEquals(1, result.skippedLogs());
        assertTrue(result.metadataComplete());
        assertEquals(List.of(WindowsEventLogWarningCode.LOG_SKIPPED), result.warnings());
        assertTrue(result.reachedCap());
        assertTrue(result.timedOut());

        List<WindowsEventLogImportedEvent> imported = result.events();
        assertSame(newerSuspicious, imported.get(0).originalEntry());
        assertSame(older, imported.get(1).originalEntry());
        assertSame(invalidFirst, imported.get(2).originalEntry());
        assertSame(missingSecond, imported.get(3).originalEntry());

        for (int index = 0; index < imported.size(); index++) {
            assertEquals(index + 1, imported.get(index).logEvent().getLineNumber());
            assertEquals(
                    WindowsEventLogIdentity.from(imported.get(index).originalEntry()),
                    imported.get(index).identity());
        }

        assertEquals(Severity.ERROR, imported.getFirst().logEvent().getSeverity());
        assertTrue(imported.getFirst().logEvent().isSuspicious());
        assertEquals("failed", imported.getFirst().logEvent().getMatchedKeyword());
        assertFalse(imported.get(1).logEvent().isSuspicious());
        assertEquals("", imported.get(2).logEvent().getTimestamp());
        assertEquals("", imported.get(3).logEvent().getTimestamp());
    }

    @Test
    void returnsControlledEmptyResultWhenRunnerReportsStartFailure() {
        WindowsEventLogCommandRunner fakeRunner = query ->
                WindowsEventLogCommandResult.empty(WindowsEventLogWarningCode.PROCESS_START_FAILED);
        WindowsEventLogImportService service = new WindowsEventLogImportService(fakeRunner);

        WindowsEventLogImportResult result = service.importEvents(WindowsEventLogQuery.last24Hours(
                Clock.fixed(Instant.parse("2026-08-06T13:00:00Z"), ZoneOffset.UTC)));

        assertTrue(result.events().isEmpty());
        assertEquals(0, result.totalEvents());
        assertEquals(0, result.suspiciousEvents());
        assertEquals(0, result.logsConsulted());
        assertEquals(0, result.logsWithEvents());
        assertEquals(0, result.skippedLogs());
        assertFalse(result.metadataComplete());
        assertEquals(
                List.of(WindowsEventLogWarningCode.PROCESS_START_FAILED),
                result.warnings());
        assertFalse(result.reachedCap());
        assertFalse(result.timedOut());
    }

    @Test
    void keepsDetectionAndStructuredWarningsForPartialResultWithoutSummary() {
        WindowsEventLogEntry suspiciousEntry = entry(
                "2026-08-06T12:00:00Z", 2, "Failed login detected", 20L);
        WindowsEventLogCommandRunner fakeRunner = query -> new WindowsEventLogCommandResult(
                List.of(suspiciousEntry),
                0,
                0,
                0,
                false,
                List.of(
                        WindowsEventLogWarningCode.SUMMARY_MISSING,
                        WindowsEventLogWarningCode.TIMEOUT),
                false,
                true);
        WindowsEventLogImportService service = new WindowsEventLogImportService(fakeRunner);

        WindowsEventLogImportResult result = service.importEvents(WindowsEventLogQuery.last24Hours(
                Clock.fixed(Instant.parse("2026-08-06T13:00:00Z"), ZoneOffset.UTC)));

        assertEquals(1, result.totalEvents());
        assertEquals(1, result.suspiciousEvents());
        assertFalse(result.metadataComplete());
        assertEquals(0, result.logsConsulted());
        assertEquals(0, result.logsWithEvents());
        assertEquals(0, result.skippedLogs());
        assertEquals(
                List.of(
                        WindowsEventLogWarningCode.SUMMARY_MISSING,
                        WindowsEventLogWarningCode.TIMEOUT),
                result.warnings());
        assertTrue(result.timedOut());
        assertSame(suspiciousEntry, result.events().getFirst().originalEntry());
        assertTrue(result.events().getFirst().logEvent().isSuspicious());
        assertEquals(1, result.events().getFirst().logEvent().getLineNumber());
        assertEquals(
                WindowsEventLogIdentity.from(suspiciousEntry),
                result.events().getFirst().identity());
    }

    private WindowsEventLogEntry entry(String timestamp, Integer level, String message, Long recordId) {
        return new WindowsEventLogEntry(
                timestamp,
                level,
                "Application",
                "Provider",
                42,
                recordId,
                "HOST",
                message,
                "<Event />");
    }
}
