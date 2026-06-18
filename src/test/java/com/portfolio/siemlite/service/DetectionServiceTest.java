package com.portfolio.siemlite.service;

import com.portfolio.siemlite.model.LogEvent;
import com.portfolio.siemlite.model.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionServiceTest {

    private final DetectionService detectionService = new DetectionService();

    @Test
    void marksSuspiciousEventsWhenKeywordIsPresent() {
        LogEvent event = new LogEvent(
                1,
                "2026-06-18 09:12:05",
                Severity.CRITICAL,
                "endpoint",
                "Malware signature detected",
                "2026-06-18 09:12:05 CRITICAL [endpoint] Malware signature detected"
        );

        detectionService.detectSuspiciousEvents(List.of(event));

        assertTrue(event.isSuspicious());
        assertEquals("critical", event.getMatchedKeyword());
    }

    @Test
    void leavesNormalEventsUnmarked() {
        LogEvent event = new LogEvent(
                1,
                "",
                Severity.INFO,
                "system",
                "Service started successfully",
                "INFO [system] Service started successfully"
        );

        detectionService.detectSuspiciousEvents(List.of(event));

        assertFalse(event.isSuspicious());
        assertEquals("", event.getMatchedKeyword());
    }
}
