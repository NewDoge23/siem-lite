package com.portfolio.siemlite.windows;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowsEventLogIdentityTest {

    @Test
    void buildsDeterministicCanonicalIdentityWithWindowsNamespace() {
        WindowsEventLogEntry entry = completeEntry(9_001L);

        WindowsEventLogIdentity first = WindowsEventLogIdentity.from(entry);
        WindowsEventLogIdentity second = WindowsEventLogIdentity.from(entry);

        assertEquals(WindowsEventLogIdentity.SOURCE_TYPE, first.sourceType());
        assertEquals(first, second);
        assertEquals(first.canonicalString(), second.canonicalString());
        assertEquals(first.hashInput(), first.canonicalString());
        assertEquals(first.contentHash(), second.contentHash());
        assertEquals(64, first.contentHash().length());
        assertTrue(first.canonicalString().contains("sourceType=17:WINDOWS_EVENT_LOG"));
        assertTrue(first.canonicalString().contains("logName=11:application"));
        assertTrue(first.canonicalString().contains("providerName=10:provider ñ"));
        assertTrue(first.canonicalString().contains("eventId=2:42"));
        assertTrue(first.canonicalString().contains("recordId=4:9001"));
        assertTrue(first.canonicalString().contains("timestamp=20:2026-08-06T12:00:00Z"));
        assertTrue(first.canonicalString().contains("computer=8:equipo-á"));
        assertTrue(first.canonicalString().contains("messageHash=0:"));
        assertFalse(first.canonicalString().contains("FILE_IMPORT"));
    }

    @Test
    void differentRecordIdsProduceDifferentIdentities() {
        WindowsEventLogIdentity first = WindowsEventLogIdentity.from(completeEntry(9_001L));
        WindowsEventLogIdentity second = WindowsEventLogIdentity.from(completeEntry(9_002L));

        assertNotEquals(first.canonicalString(), second.canonicalString());
        assertNotEquals(first.contentHash(), second.contentHash());
    }

    @Test
    void handlesNullsDeterministically() {
        WindowsEventLogEntry emptyEntry = new WindowsEventLogEntry(
                null, null, null, null, null, null, null, null, null);

        WindowsEventLogIdentity first = WindowsEventLogIdentity.from(emptyEntry);
        WindowsEventLogIdentity second = WindowsEventLogIdentity.from(emptyEntry);

        assertEquals(first.canonicalString(), second.canonicalString());
        assertEquals("", first.logName());
        assertEquals("", first.providerName());
        assertEquals("", first.timestamp());
        assertEquals("", first.computer());
        assertEquals("", first.messageHash());
    }

    @Test
    void usesContentHashOnlyAsFallbackWhenRecordIdIsMissing() {
        WindowsEventLogEntry firstEntry = entryWithoutRecordId("<Event><Data>one</Data></Event>", "Message one");
        WindowsEventLogEntry secondEntry = entryWithoutRecordId("<Event><Data>two</Data></Event>", "Message one");

        WindowsEventLogIdentity first = WindowsEventLogIdentity.from(firstEntry);
        WindowsEventLogIdentity same = WindowsEventLogIdentity.from(firstEntry);
        WindowsEventLogIdentity second = WindowsEventLogIdentity.from(secondEntry);

        assertEquals(64, first.messageHash().length());
        assertEquals(first, same);
        assertNotEquals(first.messageHash(), second.messageHash());
        assertNotEquals(first.contentHash(), second.contentHash());
    }

    @Test
    void rejectsNonWindowsSourceType() {
        assertThrows(IllegalArgumentException.class, () -> new WindowsEventLogIdentity(
                "FILE_IMPORT", "log", "provider", 1, 2L, "timestamp", "computer", ""));
    }

    private WindowsEventLogEntry completeEntry(Long recordId) {
        return new WindowsEventLogEntry(
                "2026-08-06T09:00:00-03:00",
                2,
                " Application ",
                " Provider Ñ ",
                42,
                recordId,
                " EQUIPO-Á ",
                "Formatted message that may vary",
                "<Event />");
    }

    private WindowsEventLogEntry entryWithoutRecordId(String rawXml, String message) {
        return new WindowsEventLogEntry(
                "2026-08-06T12:00:00Z",
                4,
                "Application",
                "Provider",
                42,
                null,
                "HOST",
                message,
                rawXml);
    }
}
