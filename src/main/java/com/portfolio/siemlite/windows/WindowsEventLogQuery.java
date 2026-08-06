package com.portfolio.siemlite.windows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record WindowsEventLogQuery(
        Instant startTime,
        int maxEventsPerLog,
        int maxTotalEvents,
        Duration timeout,
        long maxStdoutBytes) {

    public static final Duration DEFAULT_WINDOW = Duration.ofHours(24);
    public static final int DEFAULT_MAX_EVENTS_PER_LOG = 200;
    public static final int DEFAULT_MAX_TOTAL_EVENTS = 5_000;
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    public static final long DEFAULT_MAX_STDOUT_BYTES = 32L * 1024L * 1024L;

    public WindowsEventLogQuery {
        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(timeout, "timeout");

        if (maxEventsPerLog <= 0) {
            throw new IllegalArgumentException("maxEventsPerLog must be positive.");
        }
        if (maxTotalEvents <= 0) {
            throw new IllegalArgumentException("maxTotalEvents must be positive.");
        }
        if (maxEventsPerLog > maxTotalEvents) {
            throw new IllegalArgumentException("maxEventsPerLog cannot exceed maxTotalEvents.");
        }
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive.");
        }
        if (maxStdoutBytes <= 0) {
            throw new IllegalArgumentException("maxStdoutBytes must be positive.");
        }
        if (maxStdoutBytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("maxStdoutBytes is too large.");
        }
    }

    public static WindowsEventLogQuery last24Hours(Clock clock) {
        Objects.requireNonNull(clock, "clock");
        return new WindowsEventLogQuery(
                clock.instant().minus(DEFAULT_WINDOW),
                DEFAULT_MAX_EVENTS_PER_LOG,
                DEFAULT_MAX_TOTAL_EVENTS,
                DEFAULT_TIMEOUT,
                DEFAULT_MAX_STDOUT_BYTES);
    }
}
