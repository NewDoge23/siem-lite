package com.portfolio.siemlite.windows;

import java.util.List;
import java.util.Objects;

public record WindowsEventLogCommandResult(
        List<WindowsEventLogEntry> events,
        int logsQueried,
        int logsWithData,
        int logsSkipped,
        boolean metadataComplete,
        List<WindowsEventLogWarningCode> warnings,
        boolean capReached,
        boolean timedOut) {

    public WindowsEventLogCommandResult {
        events = List.copyOf(events);
        warnings = List.copyOf(warnings);

        if (logsQueried < 0 || logsWithData < 0 || logsSkipped < 0) {
            throw new IllegalArgumentException("Log counts cannot be negative.");
        }
        if (logsWithData > logsQueried) {
            throw new IllegalArgumentException("logsWithData cannot exceed logsQueried.");
        }
    }

    public static WindowsEventLogCommandResult empty(WindowsEventLogWarningCode warning) {
        return new WindowsEventLogCommandResult(
                List.of(),
                0,
                0,
                0,
                false,
                List.of(Objects.requireNonNull(warning, "warning")),
                false,
                false);
    }
}
