package com.portfolio.siemlite.parser;

import com.portfolio.siemlite.model.LogEvent;
import com.portfolio.siemlite.model.Severity;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenericLogParser implements LogParser {

    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2})");
    private static final Pattern SOURCE_PATTERN = Pattern.compile("\\[([^\\]]+)]");

    @Override
    public List<LogEvent> parse(Path path) throws IOException {
        List<LogEvent> events = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                events.add(parseLine(lineNumber, line));
                lineNumber++;
            }
        }

        return events;
    }

    private LogEvent parseLine(int lineNumber, String rawLine) {
        String timestamp = extractTimestamp(rawLine);
        Severity severity = detectSeverity(rawLine);
        String source = extractSource(rawLine);
        String message = extractMessage(rawLine, timestamp, severity, source);

        return new LogEvent(lineNumber, timestamp, severity, source, message, rawLine);
    }

    private String extractTimestamp(String rawLine) {
        Matcher matcher = TIMESTAMP_PATTERN.matcher(rawLine);
        return matcher.find() ? matcher.group(1) : "";
    }

    private Severity detectSeverity(String rawLine) {
        String normalizedLine = rawLine.toLowerCase(Locale.ROOT);

        if (normalizedLine.contains("critical") || normalizedLine.contains("fatal")) {
            return Severity.CRITICAL;
        }
        if (normalizedLine.contains("error")
                || normalizedLine.contains("failed")
                || normalizedLine.contains("denied")
                || normalizedLine.contains("unauthorized")) {
            return Severity.ERROR;
        }
        if (normalizedLine.contains("warn") || normalizedLine.contains("warning")) {
            return Severity.WARN;
        }
        if (normalizedLine.contains("info")) {
            return Severity.INFO;
        }
        return Severity.UNKNOWN;
    }

    private String extractSource(String rawLine) {
        Matcher matcher = SOURCE_PATTERN.matcher(rawLine);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String extractMessage(String rawLine, String timestamp, Severity severity, String source) {
        String message = rawLine;

        if (!timestamp.isBlank()) {
            message = message.replaceFirst(Pattern.quote(timestamp), "").trim();
        }
        if (severity != Severity.UNKNOWN) {
            message = message.replaceFirst("(?i)\\b" + Pattern.quote(severity.name()) + "\\b", "").trim();
        }
        if (!source.isBlank()) {
            message = message.replaceFirst("\\[" + Pattern.quote(source) + "]", "").trim();
        }

        return message.isBlank() ? rawLine : message;
    }
}
