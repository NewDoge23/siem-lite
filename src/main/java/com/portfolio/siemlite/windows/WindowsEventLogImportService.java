package com.portfolio.siemlite.windows;

import com.portfolio.siemlite.model.LogEvent;
import com.portfolio.siemlite.service.DetectionService;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class WindowsEventLogImportService {

    private final WindowsEventLogCommandRunner commandRunner;
    private final WindowsEventLogMapper mapper;
    private final DetectionService detectionService;

    public WindowsEventLogImportService(WindowsEventLogCommandRunner commandRunner) {
        this(commandRunner, new WindowsEventLogMapper(), new DetectionService());
    }

    WindowsEventLogImportService(
            WindowsEventLogCommandRunner commandRunner,
            WindowsEventLogMapper mapper,
            DetectionService detectionService) {
        this.commandRunner = Objects.requireNonNull(commandRunner, "commandRunner");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.detectionService = Objects.requireNonNull(detectionService, "detectionService");
    }

    public WindowsEventLogImportResult importEvents(WindowsEventLogQuery query) {
        Objects.requireNonNull(query, "query");
        WindowsEventLogCommandResult commandResult = Objects.requireNonNull(
                commandRunner.run(query),
                "commandRunner result");

        List<WindowsEventLogEntry> orderedEntries = orderEntries(commandResult.events());
        List<WindowsEventLogImportedEvent> importedEvents = new ArrayList<>(orderedEntries.size());
        List<LogEvent> logEvents = new ArrayList<>(orderedEntries.size());

        for (int index = 0; index < orderedEntries.size(); index++) {
            WindowsEventLogEntry entry = orderedEntries.get(index);
            LogEvent logEvent = mapper.map(entry, index + 1);
            logEvents.add(logEvent);
            importedEvents.add(new WindowsEventLogImportedEvent(
                    entry,
                    logEvent,
                    WindowsEventLogIdentity.from(entry)));
        }

        detectionService.detectSuspiciousEvents(logEvents);
        int suspiciousEvents = (int) logEvents.stream().filter(LogEvent::isSuspicious).count();

        return new WindowsEventLogImportResult(
                importedEvents,
                importedEvents.size(),
                suspiciousEvents,
                commandResult.logsQueried(),
                commandResult.logsWithData(),
                commandResult.logsSkipped(),
                commandResult.metadataComplete(),
                commandResult.warnings(),
                commandResult.capReached(),
                commandResult.timedOut());
    }

    private List<WindowsEventLogEntry> orderEntries(List<WindowsEventLogEntry> entries) {
        List<WindowsEventLogEntry> ordered = new ArrayList<>(entries);
        ordered.sort(Comparator.comparing(
                WindowsEventLogImportService::sortableTimestamp,
                WindowsEventLogImportService::compareTimestamps));
        return ordered;
    }

    private static Optional<Instant> sortableTimestamp(WindowsEventLogEntry entry) {
        String timestamp = entry.timestamp();
        if (timestamp == null || timestamp.isBlank()) {
            return Optional.empty();
        }

        String candidate = timestamp.trim();
        try {
            return Optional.of(Instant.parse(candidate));
        } catch (DateTimeParseException exception) {
            try {
                return Optional.of(OffsetDateTime.parse(candidate).toInstant());
            } catch (DateTimeParseException ignored) {
                return Optional.empty();
            }
        }
    }

    private static int compareTimestamps(Optional<Instant> first, Optional<Instant> second) {
        if (first.isPresent() && second.isPresent()) {
            return second.get().compareTo(first.get());
        }
        if (first.isPresent()) {
            return -1;
        }
        if (second.isPresent()) {
            return 1;
        }
        return 0;
    }
}
