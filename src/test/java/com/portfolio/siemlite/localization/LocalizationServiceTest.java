package com.portfolio.siemlite.localization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalizationServiceTest {

    @Test
    void loadsEnglishByDefault() {
        LocalizationService service = new LocalizationService(LanguageOption.ENGLISH);

        assertEquals("Import Log", service.get("button.importLog"));
    }

    @Test
    void loadsSpanishBundle() {
        LocalizationService service = new LocalizationService(LanguageOption.SPANISH);

        assertEquals("Importar log", service.get("button.importLog"));
    }

    @Test
    void returnsKeyWhenTranslationIsMissing() {
        LocalizationService service = new LocalizationService(LanguageOption.ENGLISH);

        assertEquals("missing.key", service.get("missing.key"));
    }

    @Test
    void formatsMessages() {
        LocalizationService service = new LocalizationService(LanguageOption.ENGLISH);

        assertEquals(
                "Visible events: 2 / 5",
                service.format("status.visibleEvents", 2, 5));
    }
}
