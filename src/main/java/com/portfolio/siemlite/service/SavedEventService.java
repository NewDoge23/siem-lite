package com.portfolio.siemlite.service;

import com.portfolio.siemlite.model.LogEvent;
import com.portfolio.siemlite.model.SavedLogEvent;
import com.portfolio.siemlite.repository.SavedEventRepository;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

public class SavedEventService {

    private final SavedEventRepository repository;

    public SavedEventService(SavedEventRepository repository) {
        this.repository = repository;
    }

    public SavedEventSaveResult saveSuspiciousEvents(List<LogEvent> events, Path importedFilePath) {
        String importedFileName = importedFilePath.getFileName().toString();
        String importedPath = importedFilePath.toAbsolutePath().toString();
        int suspiciousEvents = 0;
        int savedEvents = 0;
        int duplicateEvents = 0;

        for (LogEvent event : events) {
            if (!event.isSuspicious()) {
                continue;
            }

            suspiciousEvents++;
            SavedLogEvent savedEvent = toSavedEvent(event, importedFileName, importedPath);
            if (repository.save(savedEvent)) {
                savedEvents++;
            } else {
                duplicateEvents++;
            }
        }

        return new SavedEventSaveResult(suspiciousEvents, savedEvents, duplicateEvents);
    }

    public List<SavedLogEvent> loadSavedEvents() {
        return repository.findAll();
    }

    private SavedLogEvent toSavedEvent(LogEvent event, String importedFileName, String importedPath) {
        String savedAt = LocalDateTime.now().toString();
        String contentHash = hash(event, importedFileName);
        return new SavedLogEvent(
                0,
                event.getLineNumber(),
                event.getTimestamp(),
                event.getSeverity(),
                event.getSource(),
                event.getMessage(),
                event.getRawLine(),
                event.isSuspicious(),
                event.getMatchedKeyword(),
                importedFileName,
                importedPath,
                savedAt,
                contentHash);
    }

    private String hash(LogEvent event, String importedFileName) {
        String content = String.join("|",
                importedFileName,
                String.valueOf(event.getLineNumber()),
                nullSafe(event.getSeverity().name()),
                nullSafe(event.getSource()),
                nullSafe(event.getMatchedKeyword()),
                nullSafe(event.getRawLine()));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
