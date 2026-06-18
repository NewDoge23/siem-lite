package com.portfolio.siemlite.service;

import com.portfolio.siemlite.model.LogEvent;

import java.util.List;
import java.util.Locale;

public class EventFilterService {

    public List<LogEvent> filter(List<LogEvent> events, String searchText, String selectedSeverity) {
        String normalizedSearch = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);
        String severityFilter = selectedSeverity == null ? "Todas" : selectedSeverity;

        return events.stream()
                .filter(event -> matchesSeverity(event, severityFilter))
                .filter(event -> matchesSearch(event, normalizedSearch))
                .toList();
    }

    private boolean matchesSeverity(LogEvent event, String selectedSeverity) {
        return "Todas".equals(selectedSeverity) || event.getSeverity().name().equals(selectedSeverity);
    }

    private boolean matchesSearch(LogEvent event, String normalizedSearch) {
        if (normalizedSearch.isBlank()) {
            return true;
        }

        return contains(event.getTimestamp(), normalizedSearch)
                || contains(event.getSource(), normalizedSearch)
                || contains(event.getSeverity().name(), normalizedSearch)
                || contains(event.getMatchedKeyword(), normalizedSearch)
                || contains(event.getMessage(), normalizedSearch)
                || contains(event.getRawLine(), normalizedSearch);
    }

    private boolean contains(String value, String normalizedSearch) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedSearch);
    }
}
