package com.portfolio.siemlite.model;

public class LogEvent {

    private final int lineNumber;
    private final String timestamp;
    private final Severity severity;
    private final String source;
    private final String message;
    private final String rawLine;
    private boolean suspicious;
    private String matchedKeyword;

    public LogEvent(int lineNumber, String timestamp, Severity severity, String source, String message, String rawLine) {
        this.lineNumber = lineNumber;
        this.timestamp = timestamp;
        this.severity = severity;
        this.source = source;
        this.message = message;
        this.rawLine = rawLine;
        this.suspicious = false;
        this.matchedKeyword = "";
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

    public void setSuspicious(boolean suspicious) {
        this.suspicious = suspicious;
    }

    public String getSuspiciousLabel() {
        return suspicious ? "Yes" : "No";
    }

    public String getMatchedKeyword() {
        return matchedKeyword;
    }

    public void setMatchedKeyword(String matchedKeyword) {
        this.matchedKeyword = matchedKeyword == null ? "" : matchedKeyword;
    }
}
