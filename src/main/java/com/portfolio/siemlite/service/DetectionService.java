package com.portfolio.siemlite.service;

import com.portfolio.siemlite.model.LogEvent;

import java.util.List;
import java.util.Locale;

public class DetectionService {

    private static final List<String> SUSPICIOUS_KEYWORDS = List.of(
            "failed",
            "denied",
            "unauthorized",
            "error",
            "critical",
            "malware",
            "bruteforce"
    );

    public void detectSuspiciousEvents(List<LogEvent> events) {
        for (LogEvent event : events) {
            detectSuspiciousEvent(event);
        }
    }

    private void detectSuspiciousEvent(LogEvent event) {
        String normalizedLine = event.getRawLine().toLowerCase(Locale.ROOT);

        for (String keyword : SUSPICIOUS_KEYWORDS) {
            if (normalizedLine.contains(keyword)) {
                event.setSuspicious(true);
                event.setMatchedKeyword(keyword);
                return;
            }
        }

        event.setSuspicious(false);
        event.setMatchedKeyword("");
    }
}
