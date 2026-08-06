package com.portfolio.siemlite.windows;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WindowsEventLogQueryTest {

    @Test
    void createsLast24HoursQueryFromFixedClock() {
        Instant now = Instant.parse("2026-08-06T15:30:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);

        WindowsEventLogQuery query = WindowsEventLogQuery.last24Hours(clock);

        assertEquals(Instant.parse("2026-08-05T15:30:00Z"), query.startTime());
        assertEquals(Duration.ofHours(24), WindowsEventLogQuery.DEFAULT_WINDOW);
        assertEquals(200, query.maxEventsPerLog());
        assertEquals(5_000, query.maxTotalEvents());
        assertEquals(Duration.ofSeconds(60), query.timeout());
        assertEquals(32L * 1024L * 1024L, query.maxStdoutBytes());
    }

    @Test
    void rejectsInvalidCapsAndTimeouts() {
        Instant startTime = Instant.parse("2026-08-05T15:30:00Z");

        assertThrows(IllegalArgumentException.class, () -> query(startTime, 0, 10, Duration.ofSeconds(1), 1_024));
        assertThrows(IllegalArgumentException.class, () -> query(startTime, 1, 0, Duration.ofSeconds(1), 1_024));
        assertThrows(IllegalArgumentException.class, () -> query(startTime, 11, 10, Duration.ofSeconds(1), 1_024));
        assertThrows(IllegalArgumentException.class, () -> query(startTime, 1, 10, Duration.ZERO, 1_024));
        assertThrows(IllegalArgumentException.class, () -> query(startTime, 1, 10, Duration.ofSeconds(1), 0));
        assertThrows(IllegalArgumentException.class, () -> query(
                startTime,
                1,
                10,
                Duration.ofSeconds(1),
                (long) Integer.MAX_VALUE + 1));
    }

    private WindowsEventLogQuery query(
            Instant startTime,
            int maxPerLog,
            int maxTotal,
            Duration timeout,
            long stdoutLimit) {
        return new WindowsEventLogQuery(startTime, maxPerLog, maxTotal, timeout, stdoutLimit);
    }
}
