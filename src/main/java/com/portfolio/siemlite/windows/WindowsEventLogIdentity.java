package com.portfolio.siemlite.windows;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

public record WindowsEventLogIdentity(
        String sourceType,
        String logName,
        String providerName,
        Integer eventId,
        Long recordId,
        String timestamp,
        String computer,
        String messageHash) {

    public static final String SOURCE_TYPE = "WINDOWS_EVENT_LOG";

    public WindowsEventLogIdentity {
        if (!SOURCE_TYPE.equals(sourceType)) {
            throw new IllegalArgumentException("sourceType must be WINDOWS_EVENT_LOG.");
        }
        logName = normalizeIdentityText(logName);
        providerName = normalizeIdentityText(providerName);
        timestamp = normalizeTimestamp(timestamp);
        computer = normalizeIdentityText(computer);
        messageHash = messageHash == null ? "" : messageHash.trim().toLowerCase(Locale.ROOT);
    }

    public static WindowsEventLogIdentity from(WindowsEventLogEntry entry) {
        Objects.requireNonNull(entry, "entry");
        return new WindowsEventLogIdentity(
                SOURCE_TYPE,
                entry.logName(),
                entry.providerName(),
                entry.eventId(),
                entry.recordId(),
                entry.timestamp(),
                entry.computer(),
                fallbackMessageHash(entry));
    }

    public String canonicalString() {
        return String.join("|",
                component("sourceType", sourceType),
                component("logName", logName),
                component("providerName", providerName),
                component("eventId", number(eventId)),
                component("recordId", number(recordId)),
                component("timestamp", timestamp),
                component("computer", computer),
                component("messageHash", messageHash));
    }

    public String hashInput() {
        return canonicalString();
    }

    public String contentHash() {
        return sha256(hashInput());
    }

    private static String fallbackMessageHash(WindowsEventLogEntry entry) {
        if (entry.recordId() != null) {
            return "";
        }

        String fallbackContent = firstNonBlank(entry.rawXml(), entry.message());
        return fallbackContent.isEmpty() ? "" : sha256(fallbackContent);
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback == null || fallback.isBlank() ? "" : fallback;
    }

    private static String normalizeIdentityText(String value) {
        return value == null
                ? ""
                : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String normalizeTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String candidate = value.trim();
        try {
            return Instant.parse(candidate).toString();
        } catch (DateTimeParseException exception) {
            try {
                return OffsetDateTime.parse(candidate).toInstant().toString();
            } catch (DateTimeParseException ignored) {
                return candidate;
            }
        }
    }

    private static String component(String name, String value) {
        return name + "=" + value.length() + ":" + value;
    }

    private static String number(Number value) {
        return value == null ? "" : value.toString();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
