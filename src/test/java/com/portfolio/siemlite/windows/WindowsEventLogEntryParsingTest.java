package com.portfolio.siemlite.windows;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowsEventLogEntryParsingTest {

    private final PowerShellWindowsEventLogCommandRunner runner =
            new PowerShellWindowsEventLogCommandRunner();

    @Test
    void parsesNdjsonWithUnicodeAndAccents() {
        String output = """
                {"type":"event","timestamp":"2026-08-06T12:00:00.0000000Z","level":2,"logName":"System","providerName":"Proveedor Ñ","eventId":42,"recordId":9001,"computer":"EQUIPO-Á","message":"Acceso denegado – José","rawXml":"<Event>áéíóú</Event>"}
                {"type":"summary","logsQueried":1,"logsWithData":1,"logsSkipped":0,"capReached":false}
                """;

        PowerShellWindowsEventLogCommandRunner.ParsedOutput parsed = runner.parseOutput(output);

        assertEquals(1, parsed.events().size());
        WindowsEventLogEntry entry = parsed.events().getFirst();
        assertEquals("2026-08-06T12:00:00.0000000Z", entry.timestamp());
        assertEquals(2, entry.level());
        assertEquals("System", entry.logName());
        assertEquals("Proveedor Ñ", entry.providerName());
        assertEquals(42, entry.eventId());
        assertEquals(9001L, entry.recordId());
        assertEquals("EQUIPO-Á", entry.computer());
        assertEquals("Acceso denegado – José", entry.message());
        assertEquals("<Event>áéíóú</Event>", entry.rawXml());
        assertTrue(parsed.warnings().isEmpty());
    }

    @Test
    void acceptsNullAndMissingEventFields() {
        String output = """
                {"type":"event","timestamp":null,"level":null,"logName":"Application","message":null}
                {"type":"summary","logsQueried":1,"logsWithData":1,"logsSkipped":0,"capReached":false}
                """;

        WindowsEventLogEntry entry = runner.parseOutput(output).events().getFirst();

        assertNull(entry.timestamp());
        assertNull(entry.level());
        assertEquals("Application", entry.logName());
        assertNull(entry.providerName());
        assertNull(entry.eventId());
        assertNull(entry.recordId());
        assertNull(entry.computer());
        assertNull(entry.message());
        assertNull(entry.rawXml());
    }
}
