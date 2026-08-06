package com.portfolio.siemlite.windows;

import com.portfolio.siemlite.model.LogEvent;

import java.util.Objects;

public record WindowsEventLogImportedEvent(
        WindowsEventLogEntry originalEntry,
        LogEvent logEvent,
        WindowsEventLogIdentity identity) {

    public WindowsEventLogImportedEvent {
        Objects.requireNonNull(originalEntry, "originalEntry");
        Objects.requireNonNull(logEvent, "logEvent");
        Objects.requireNonNull(identity, "identity");
    }
}
