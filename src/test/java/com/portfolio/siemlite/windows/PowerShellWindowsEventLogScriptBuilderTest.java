package com.portfolio.siemlite.windows;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerShellWindowsEventLogScriptBuilderTest {

    @Test
    void buildsBoundedTimeFilteredAllLogsScript() {
        String script = builder().build(query());

        assertTrue(script.contains("Get-WinEvent -ListLog '*'"));
        assertTrue(script.contains("Get-WinEvent -FilterHashtable"));
        assertTrue(script.contains("StartTime = $startTime"));
        assertTrue(script.contains("-MaxEvents $maxForLog"));
        assertTrue(script.contains("-ErrorAction SilentlyContinue -ErrorVariable +queryErrors"));
        assertTrue(script.contains("if ($queryErrors.Count -gt 0 -and $events.Count -eq 0)"));
        assertTrue(script.contains("$logsSkipped += 1"));
        assertFalse(script.contains("-ErrorAction Stop"));
        assertTrue(script.contains("[DateTime]::ParseExact('2026-08-05T12:30:00.000'"));
        assertTrue(script.contains("'yyyy-MM-ddTHH:mm:ss.fff'"));
        assertTrue(script.contains("[Globalization.DateTimeStyles]::AssumeLocal"));
        assertFalse(script.contains("2026-08-05T15:30:00Z"));
        assertFalse(script.contains(".UtcDateTime"));
        assertTrue(script.contains("$maxEventsPerLog = 25"));
        assertTrue(script.contains("$maxTotalEvents = 100"));
        assertFalse(script.contains("Where-Object"));
        assertFalse(script.contains("Get-WinEvent -LogName"));
        assertFalse(script.contains("'Security'"));
        assertFalse(script.contains("'Application'"));
        assertFalse(script.contains("'System'"));
        assertFalse(script.contains("__SIEM_"));
    }

    @Test
    void suppressesOnlyTheRunnersOwnPowerShellAuditRecords() {
        String script = builder().build(query());

        assertFalse(script.contains("$ErrorActionPreference"));
        assertTrue(script.contains(PowerShellWindowsEventLogScriptBuilder.INTERNAL_RUNNER_MARKER));
        assertTrue(script.contains("$event.ProviderName -eq 'Microsoft-Windows-PowerShell'"));
        assertTrue(script.contains("$event.Id -eq 4100 -or $event.Id -eq 4104"));
        assertTrue(script.contains("$message.Contains($siemLiteRunnerMarker)"));
        assertTrue(script.contains("$message.Contains('Get-WinEvent -ListLog ''*''')"));
        assertTrue(script.contains("$message.Contains('$maxTotalEvents')"));
        assertTrue(script.contains("$message.Contains('type = ''summary''')"));
        assertTrue(script.contains("$event.ProviderName -eq 'Microsoft-Windows-PowerShell' -and $isPowerShellRunnerEventId -and"));
        assertTrue(script.contains("$hasCurrentRunnerMarker -or $hasLegacyRunnerSignature"));
        assertTrue(script.contains("if ($isSiemLiteRunnerAuditEvent)"));
        assertFalse(script.contains("$message.Contains(\""));
        assertFalse(script.contains("$log.LogName -ne 'Microsoft-Windows-PowerShell/Operational'"));
        assertFalse(script.contains("$event.ProviderName -ne 'Microsoft-Windows-PowerShell'"));
        assertFalse(script.contains("$message.Contains('ErrorActionPreference')"));
        assertFalse(script.contains("$message.Contains('error')"));
    }

    private PowerShellWindowsEventLogScriptBuilder builder() {
        return new PowerShellWindowsEventLogScriptBuilder(
                ZoneId.of("America/Argentina/Buenos_Aires"));
    }

    private WindowsEventLogQuery query() {
        return new WindowsEventLogQuery(
                Instant.parse("2026-08-05T15:30:00Z"),
                25,
                100,
                Duration.ofSeconds(5),
                1_024);
    }
}
