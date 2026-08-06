package com.portfolio.siemlite.windows;

import com.portfolio.siemlite.model.LogEvent;
import com.portfolio.siemlite.model.Severity;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public class WindowsEventLogMapper {

    static final String DEFAULT_SOURCE = "Windows Event Log";
    static final String DEFAULT_MESSAGE = "No formatted message available.";
    static final int MAX_MESSAGE_LENGTH = 4_096;

    private static final DateTimeFormatter LOG_EVENT_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    public LogEvent map(WindowsEventLogEntry entry, int lineNumber) {
        Objects.requireNonNull(entry, "entry");
        if (lineNumber <= 0) {
            throw new IllegalArgumentException("lineNumber must be positive.");
        }

        String timestamp = normalizeTimestamp(entry.timestamp());
        Severity severity = mapSeverity(entry.level());
        String source = selectSource(entry.providerName(), entry.logName());
        String message = selectMessage(entry.message());
        String rawLine = buildRawLine(entry, timestamp, message);

        return new LogEvent(lineNumber, timestamp, severity, source, message, rawLine);
    }

    Severity mapSeverity(Integer level) {
        if (level == null) {
            return Severity.INFO;
        }

        return switch (level) {
            case 1 -> Severity.CRITICAL;
            case 2 -> Severity.ERROR;
            case 3 -> Severity.WARN;
            default -> Severity.INFO;
        };
    }

    String normalizeTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return "";
        }

        String candidate = timestamp.trim();
        try {
            return LOG_EVENT_TIMESTAMP_FORMAT.format(Instant.parse(candidate));
        } catch (DateTimeParseException exception) {
            try {
                return LOG_EVENT_TIMESTAMP_FORMAT.format(OffsetDateTime.parse(candidate).toInstant());
            } catch (DateTimeParseException ignored) {
                return "";
            }
        }
    }

    private String selectSource(String providerName, String logName) {
        if (providerName != null && !providerName.isBlank()) {
            return compact(providerName);
        }
        if (logName != null && !logName.isBlank()) {
            return compact(logName);
        }
        return DEFAULT_SOURCE;
    }

    private String selectMessage(String message) {
        if (message == null || message.isBlank()) {
            return DEFAULT_MESSAGE;
        }

        String compactMessage = compact(message);
        if (compactMessage.length() <= MAX_MESSAGE_LENGTH) {
            return compactMessage;
        }
        return compactMessage.substring(0, MAX_MESSAGE_LENGTH - 1) + "…";
    }

    private String buildRawLine(WindowsEventLogEntry entry, String timestamp, String message) {
        return String.join(" | ",
                "timestamp=" + timestamp,
                "logName=" + compact(entry.logName()),
                "providerName=" + compact(entry.providerName()),
                "eventId=" + number(entry.eventId()),
                "recordId=" + number(entry.recordId()),
                "level=" + number(entry.level()),
                "computer=" + compact(entry.computer()),
                "message=" + message);
    }

    private String compact(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String number(Number value) {
        return value == null ? "" : value.toString();
    }
}
