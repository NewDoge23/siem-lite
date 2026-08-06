package com.portfolio.siemlite.windows;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerShellWindowsEventLogScriptBuilderTest {

    @Test
    void buildsBoundedTimeFilteredAllLogsScript() {
        WindowsEventLogQuery query = new WindowsEventLogQuery(
                Instant.parse("2026-08-05T15:30:00Z"),
                25,
                100,
                Duration.ofSeconds(5),
                1_024);

        String script = new PowerShellWindowsEventLogScriptBuilder().build(query);

        assertTrue(script.contains("Get-WinEvent -ListLog '*'"));
        assertTrue(script.contains("Get-WinEvent -FilterHashtable"));
        assertTrue(script.contains("StartTime = $startTime"));
        assertTrue(script.contains("-MaxEvents $maxForLog"));
        assertTrue(script.contains("2026-08-05T15:30:00Z"));
        assertTrue(script.contains("$maxEventsPerLog = 25"));
        assertTrue(script.contains("$maxTotalEvents = 100"));
        assertFalse(script.contains("Where-Object"));
        assertFalse(script.contains("Get-WinEvent -LogName"));
        assertFalse(script.contains("'Security'"));
        assertFalse(script.contains("'Application'"));
        assertFalse(script.contains("'System'"));
        assertFalse(script.contains("__SIEM_"));
    }
}
