package com.portfolio.siemlite.service;

import com.portfolio.siemlite.model.LogEvent;
import com.portfolio.siemlite.model.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventFilterServiceTest {

    private final EventFilterService filterService = new EventFilterService();

    @Test
    void filtersByTextAndSeverity() {
        LogEvent infoEvent = new LogEvent(1, "", Severity.INFO, "system",
                "Service started", "INFO [system] Service started");
        LogEvent errorEvent = new LogEvent(2, "", Severity.ERROR, "auth",
                "Failed login for admin", "ERROR [auth] Failed login for admin");

        List<LogEvent> filtered = filterService.filter(
                List.of(infoEvent, errorEvent),
                "admin",
                Severity.ERROR.name()
        );

        assertEquals(1, filtered.size());
        assertEquals(errorEvent, filtered.getFirst());
    }

    @Test
    void returnsAllEventsWhenFiltersAreEmpty() {
        List<LogEvent> events = List.of(
                new LogEvent(1, "", Severity.INFO, "system", "Service started", "INFO Service started"),
                new LogEvent(2, "", Severity.WARN, "network", "High latency", "WARN High latency")
        );

        List<LogEvent> filtered = filterService.filter(events, "", "All");

        assertEquals(2, filtered.size());
    }
}
