package com.portfolio.siemlite.parser;

import com.portfolio.siemlite.model.LogEvent;
import com.portfolio.siemlite.model.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenericLogParserTest {

    private final GenericLogParser parser = new GenericLogParser();

    @TempDir
    Path tempDir;

    @Test
    void parsesGenericLogLines() throws IOException {
        Path logFile = tempDir.resolve("sample.log");
        Files.writeString(logFile, """
                2026-06-18 09:05:18 ERROR [auth] Failed login for user admin
                Plain line without known severity
                """);

        List<LogEvent> events = parser.parse(logFile);

        assertEquals(2, events.size());
        assertEquals(1, events.getFirst().getLineNumber());
        assertEquals("2026-06-18 09:05:18", events.getFirst().getTimestamp());
        assertEquals(Severity.ERROR, events.getFirst().getSeverity());
        assertEquals("auth", events.getFirst().getSource());
        assertEquals(Severity.UNKNOWN, events.get(1).getSeverity());
    }
}
