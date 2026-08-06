package com.portfolio.siemlite.windows;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerShellWindowsEventLogScriptBuilderTest {

    @Test
    void buildsBoundedTimeFilteredAllLogsScript() {
        String script = new PowerShellWindowsEventLogScriptBuilder().build(query());

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

    @Test
    void suppressesOnlyTheRunnersOwnPowerShellScriptBlockAuditEvent() {
        String script = new PowerShellWindowsEventLogScriptBuilder().build(query());

        assertFalse(script.contains("$ErrorActionPreference"));
        assertTrue(script.contains(PowerShellWindowsEventLogScriptBuilder.INTERNAL_RUNNER_MARKER));
        assertTrue(script.contains("$event.ProviderName -eq 'Microsoft-Windows-PowerShell'"));
        assertTrue(script.contains("$event.Id -eq 4104"));
        assertTrue(script.contains("$message.Contains($siemLiteRunnerMarker)"));
        assertTrue(script.contains("$message.Contains(\"Get-WinEvent -ListLog '*'\""));
        assertTrue(script.contains("$message.Contains('$maxTotalEvents')"));
        assertTrue(script.contains("$message.Contains(\"type = 'summary'\")"));
        assertTrue(script.contains("$isPowerShellScriptBlock -and"));
        assertTrue(script.contains("$hasCurrentRunnerMarker -or $hasLegacyRunnerSignature"));
        assertTrue(script.contains("if ($isSiemLiteRunnerAuditEvent)"));
        assertFalse(script.contains("$log.LogName -ne 'Microsoft-Windows-PowerShell/Operational'"));
        assertFalse(script.contains("$event.ProviderName -ne 'Microsoft-Windows-PowerShell'"));
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
