package com.portfolio.siemlite.windows;

import java.util.List;

public record WindowsEventLogImportResult(
        List<WindowsEventLogImportedEvent> events,
        int totalEvents,
        int suspiciousEvents,
        int logsConsulted,
        int logsWithEvents,
        int skippedLogs,
        List<String> warnings,
        boolean reachedCap,
        boolean timedOut) {

    public WindowsEventLogImportResult {
        events = List.copyOf(events);
        warnings = List.copyOf(warnings);

        if (totalEvents != events.size()) {
            throw new IllegalArgumentException("totalEvents must match the event list size.");
        }
        if (suspiciousEvents < 0 || suspiciousEvents > totalEvents) {
            throw new IllegalArgumentException("suspiciousEvents must be within the event count.");
        }
        if (logsConsulted < 0 || logsWithEvents < 0 || skippedLogs < 0) {
            throw new IllegalArgumentException("Log counts cannot be negative.");
        }
        if (logsWithEvents > logsConsulted) {
            throw new IllegalArgumentException("logsWithEvents cannot exceed logsConsulted.");
        }
    }
}
