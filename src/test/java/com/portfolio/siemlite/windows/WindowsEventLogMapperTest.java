package com.portfolio.siemlite.windows;

import com.portfolio.siemlite.model.LogEvent;
import com.portfolio.siemlite.model.Severity;
import com.portfolio.siemlite.service.DetectionService;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowsEventLogMapperTest {

    private final WindowsEventLogMapper mapper = new WindowsEventLogMapper(ZoneOffset.UTC);

    @Test
    void mapsCompleteWindowsEntryToLogEvent() {
        WindowsEventLogEntry entry = new WindowsEventLogEntry(
                "2026-08-06T12:34:56.789Z",
                2,
                "Application",
                " Proveedor Ñ ",
                42,
                9_001L,
                "EQUIPO-Á",
                "Acceso denegado\npara José",
                "<Event><Secret>not-for-log-event</Secret></Event>");

        LogEvent event = mapper.map(entry, 7);

        assertEquals(7, event.getLineNumber());
        assertEquals("2026-08-06 12:34:56", event.getTimestamp());
        assertEquals(Severity.ERROR, event.getSeverity());
        assertEquals("Proveedor Ñ", event.getSource());
        assertEquals("Acceso denegado para José", event.getMessage());
        assertTrue(event.getRawLine().contains("logName=Application"));
        assertTrue(event.getRawLine().contains("providerName=Proveedor Ñ"));
        assertTrue(event.getRawLine().contains("eventId=42"));
        assertTrue(event.getRawLine().contains("recordId=9001"));
        assertTrue(event.getRawLine().contains("computer=EQUIPO-Á"));
        assertFalse(event.getRawLine().contains("<Secret>"));
        assertFalse(event.isSuspicious());
    }

    @Test
    void rendersUtcWindowsTimestampInConfiguredLocalZoneWithoutChangingIdentity() {
        WindowsEventLogEntry entry = new WindowsEventLogEntry(
                "2026-08-06T08:05:15Z",
                4,
                "System",
                "Provider",
                42,
                9_001L,
                "HOST",
                "Message",
                null);
        WindowsEventLogMapper localMapper =
                new WindowsEventLogMapper(ZoneId.of("America/Argentina/Buenos_Aires"));

        LogEvent event = localMapper.map(entry, 1);
        WindowsEventLogIdentity identity = WindowsEventLogIdentity.from(entry);

        assertEquals("2026-08-06 05:05:15", event.getTimestamp());
        assertEquals("2026-08-06T08:05:15Z", identity.timestamp());
    }

    @Test
    void mapsSupportedAndFallbackSeverityLevels() {
        Map<Integer, Severity> expectedByLevel = Map.of(
                1, Severity.CRITICAL,
                2, Severity.ERROR,
                3, Severity.WARN,
                4, Severity.INFO,
                5, Severity.INFO,
                0, Severity.INFO,
                99, Severity.INFO);

        for (Map.Entry<Integer, Severity> expected : expectedByLevel.entrySet()) {
            LogEvent event = mapper.map(entryWithLevel(expected.getKey()), 1);
            assertEquals(expected.getValue(), event.getSeverity(), "level=" + expected.getKey());
        }

        assertEquals(Severity.INFO, mapper.map(entryWithLevel(null), 1).getSeverity());
    }

    @Test
    void selectsProviderThenLogNameThenStableSourceFallback() {
        assertEquals("Provider", mapper.map(entryWithSource("Provider", "System"), 1).getSource());
        assertEquals("System", mapper.map(entryWithSource("  ", "System"), 1).getSource());
        assertEquals(
                WindowsEventLogMapper.DEFAULT_SOURCE,
                mapper.map(entryWithSource(null, " "), 1).getSource());
    }

    @Test
    void handlesMissingMessageAndInvalidTimestampWithoutUsingLargeXml() {
        String largeRawXml = "<Event>" + "sensitive".repeat(20_000) + "</Event>";
        WindowsEventLogEntry entry = new WindowsEventLogEntry(
                "not-a-timestamp",
                null,
                null,
                null,
                null,
                null,
                null,
                "   ",
                largeRawXml);

        LogEvent event = mapper.map(entry, 1);

        assertEquals("", event.getTimestamp());
        assertEquals(Severity.INFO, event.getSeverity());
        assertEquals(WindowsEventLogMapper.DEFAULT_SOURCE, event.getSource());
        assertEquals(WindowsEventLogMapper.DEFAULT_MESSAGE, event.getMessage());
        assertFalse(event.getRawLine().contains("<Event>"));
        assertTrue(event.getRawLine().length() < 1_000);
    }

    @Test
    void boundsVeryLargeFormattedMessages() {
        WindowsEventLogEntry entry = new WindowsEventLogEntry(
                null,
                4,
                "System",
                null,
                null,
                null,
                null,
                "x".repeat(WindowsEventLogMapper.MAX_MESSAGE_LENGTH + 100),
                null);

        LogEvent event = mapper.map(entry, 1);

        assertEquals(WindowsEventLogMapper.MAX_MESSAGE_LENGTH, event.getMessage().length());
        assertTrue(event.getMessage().endsWith("…"));
    }

    @Test
    void preservesSuspiciousKeywordsBeyondVisibleMessageLimitForDetection() {
        String longMessage = "x".repeat(WindowsEventLogMapper.MAX_MESSAGE_LENGTH + 100)
                + " malware detected";
        WindowsEventLogEntry entry = new WindowsEventLogEntry(
                "2026-08-06T12:34:56Z",
                4,
                "System",
                "Provider",
                10,
                20L,
                "HOST",
                longMessage,
                "<Event>raw XML remains excluded</Event>");
        LogEvent event = mapper.map(entry, 1);

        new DetectionService().detectSuspiciousEvents(List.of(event));

        assertEquals(WindowsEventLogMapper.MAX_MESSAGE_LENGTH, event.getMessage().length());
        assertFalse(event.getMessage().contains("malware"));
        assertTrue(event.getRawLine().contains("malware detected"));
        assertFalse(event.getRawLine().contains("raw XML remains excluded"));
        assertTrue(event.isSuspicious());
        assertEquals("malware", event.getMatchedKeyword());
    }

    @Test
    void mappedRawLineRemainsCompatibleWithSuspiciousDetection() {
        WindowsEventLogEntry entry = new WindowsEventLogEntry(
                "2026-08-06T12:34:56Z",
                2,
                "Security-Audit",
                "Provider",
                100,
                200L,
                "HOST",
                "Failed login detected",
                null);
        LogEvent event = mapper.map(entry, 1);

        new DetectionService().detectSuspiciousEvents(List.of(event));

        assertTrue(event.isSuspicious());
        assertEquals("failed", event.getMatchedKeyword());
    }

    @Test
    void rejectsNonPositiveSyntheticLineNumbers() {
        assertThrows(IllegalArgumentException.class, () -> mapper.map(entryWithLevel(4), 0));
    }

    private WindowsEventLogEntry entryWithLevel(Integer level) {
        return new WindowsEventLogEntry(null, level, "System", "Provider", 1, 2L, "HOST", "Message", null);
    }

    private WindowsEventLogEntry entryWithSource(String providerName, String logName) {
        return new WindowsEventLogEntry(null, 4, logName, providerName, 1, 2L, "HOST", "Message", null);
    }
}
