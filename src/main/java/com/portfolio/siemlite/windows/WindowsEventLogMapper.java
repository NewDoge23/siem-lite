package com.portfolio.siemlite.windows;

import com.portfolio.siemlite.model.LogEvent;
import com.portfolio.siemlite.model.Severity;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public class WindowsEventLogMapper {

    static final String DEFAULT_SOURCE = "Windows Event Log";
    static final String DEFAULT_MESSAGE = "No formatted message available.";
    static final int MAX_MESSAGE_LENGTH = 4_096;

    private final DateTimeFormatter displayTimestampFormat;

    public WindowsEventLogMapper() {
        this(ZoneId.systemDefault());
    }

    public WindowsEventLogMapper(ZoneId displayZone) {
        this.displayTimestampFormat = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(Objects.requireNonNull(displayZone, "displayZone"));
    }

    public LogEvent map(WindowsEventLogEntry entry, int lineNumber) {
        Objects.requireNonNull(entry, "entry");
        if (lineNumber <= 0) {
            throw new IllegalArgumentException("lineNumber must be positive.");
        }

        String timestamp = normalizeTimestamp(entry.timestamp());
        Severity severity = mapSeverity(entry.level());
        String source = selectSource(entry.providerName(), entry.logName());
        String detectionMessage = selectDetectionMessage(entry.message());
        String message = limitVisibleMessage(detectionMessage);
        String rawLine = buildRawLine(entry, timestamp, detectionMessage);

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
            return displayTimestampFormat.format(Instant.parse(candidate));
        } catch (DateTimeParseException exception) {
            try {
                return displayTimestampFormat.format(OffsetDateTime.parse(candidate).toInstant());
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

    private String selectDetectionMessage(String message) {
        if (message == null || message.isBlank()) {
            return DEFAULT_MESSAGE;
        }

        return compact(message);
    }

    private String limitVisibleMessage(String message) {
        if (message.length() <= MAX_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_MESSAGE_LENGTH - 1) + "…";
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
