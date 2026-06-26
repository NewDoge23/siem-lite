package com.portfolio.siemlite.localization;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class BundleKeysTest {

    private static final List<String> REQUIRED_KEYS = List.of(
            "app.title",
            "button.importLog",
            "filter.search.placeholder",
            "filter.severity.all",
            "language.label",
            "language.restartRequired",
            "language.saveError",
            "tab.importedEvents",
            "tab.savedEvents",
            "table.lineNumber",
            "table.timestamp",
            "table.severity",
            "table.source",
            "table.suspicious",
            "table.keyword",
            "table.message",
            "table.savedAt",
            "table.file",
            "dialog.import.title",
            "dialog.import.extension",
            "status.initial",
            "status.visibleEvents",
            "status.importedFile",
            "status.savedSummary",
            "status.saveWarning",
            "status.loadWarning",
            "status.autoSaveUnavailable",
            "status.importError",
            "status.persistenceUnavailable",
            "warning.saveSuspiciousEvents",
            "warning.loadSavedEvents"
    );

    @Test
    void englishAndSpanishBundlesContainRequiredKeys() {
        LocalizationService english = new LocalizationService(LanguageOption.ENGLISH);
        LocalizationService spanish = new LocalizationService(LanguageOption.SPANISH);

        for (String key : REQUIRED_KEYS) {
            assertFalse(english.getBundle().getString(key).isBlank(), key);
            assertFalse(spanish.getBundle().getString(key).isBlank(), key);
        }
    }
}
