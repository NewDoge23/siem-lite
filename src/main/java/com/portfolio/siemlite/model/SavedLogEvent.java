package com.portfolio.siemlite.model;

public class SavedLogEvent {

    private final long id;
    private final int lineNumber;
    private final String timestamp;
    private final Severity severity;
    private final String source;
    private final String message;
    private final String rawLine;
    private final boolean suspicious;
    private final String matchedKeyword;
    private final String importedFileName;
    private final String importedFilePath;
    private final String savedAt;
    private final String contentHash;

    public SavedLogEvent(
            long id,
            int lineNumber,
            String timestamp,
            Severity severity,
            String source,
            String message,
            String rawLine,
            boolean suspicious,
            String matchedKeyword,
            String importedFileName,
            String importedFilePath,
            String savedAt,
            String contentHash) {
        this.id = id;
        this.lineNumber = lineNumber;
        this.timestamp = timestamp;
        this.severity = severity;
        this.source = source;
        this.message = message;
        this.rawLine = rawLine;
        this.suspicious = suspicious;
        this.matchedKeyword = matchedKeyword == null ? "" : matchedKeyword;
        this.importedFileName = importedFileName;
        this.importedFilePath = importedFilePath;
        this.savedAt = savedAt;
        this.contentHash = contentHash;
    }

    public long getId() {
        return id;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getSource() {
        return source;
    }

    public String getMessage() {
        return message;
    }

    public String getRawLine() {
        return rawLine;
    }

    public boolean isSuspicious() {
        return suspicious;
    }

    public String getSuspiciousLabel() {
        return suspicious ? "Yes" : "No";
    }

    public String getMatchedKeyword() {
        return matchedKeyword;
    }

    public String getImportedFileName() {
        return importedFileName;
    }

    public String getImportedFilePath() {
        return importedFilePath;
    }

    public String getSavedAt() {
        return savedAt;
    }

    public String getContentHash() {
        return contentHash;
    }
}
