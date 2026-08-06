package com.portfolio.siemlite.windows;

public record WindowsEventLogEntry(
        String timestamp,
        Integer level,
        String logName,
        String providerName,
        Integer eventId,
        Long recordId,
        String computer,
        String message,
        String rawXml) {
}
