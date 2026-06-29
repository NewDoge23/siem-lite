package com.portfolio.siemlite.localization;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.ResourceBundle;

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
    void loadsSpanishAccentedCopyAsUtf8() {
        LocalizationService service = new LocalizationService(LanguageOption.SPANISH);

        assertEquals("Sí", service.get("value.yes"));
        assertEquals("Guardado automático no disponible", service.get("status.autoSaveUnavailable"));
        assertEquals(
                "Idioma actualizado. Reiniciá la aplicación para aplicar todos los cambios de la interfaz.",
                service.get("language.restartRequired"));
    }

    @Test
    void returnsKeyWhenTranslationIsMissing() {
        LocalizationService service = new LocalizationService(LanguageOption.ENGLISH);

        assertEquals("missing.key", service.get("missing.key"));
    }

    @Test
    void resourceBundleFallsBackToEnglishForMissingSelectedLanguageKey() {
        ResourceBundle selectedBundle = new MapResourceBundle(Map.of("shared.key", "Texto traducido"));
        ResourceBundle englishBundle = new MapResourceBundle(Map.of(
                "shared.key", "English text",
                "english.only.key", "English fallback"));
        LocalizationService service = new LocalizationService(selectedBundle, englishBundle);

        assertEquals("Texto traducido", service.getBundle().getString("shared.key"));
        assertEquals("English fallback", service.getBundle().getString("english.only.key"));
    }

    @Test
    void resourceBundleReturnsKeyWhenMissingEverywhere() {
        ResourceBundle selectedBundle = new MapResourceBundle(Map.of());
        ResourceBundle englishBundle = new MapResourceBundle(Map.of());
        LocalizationService service = new LocalizationService(selectedBundle, englishBundle);

        assertEquals("missing.key", service.getBundle().getString("missing.key"));
    }

    @Test
    void formatsMessages() {
        LocalizationService service = new LocalizationService(LanguageOption.ENGLISH);

        assertEquals(
                "Visible events: 2 / 5",
                service.format("status.visibleEvents", 2, 5));
    }

    private static final class MapResourceBundle extends ResourceBundle {

        private final Map<String, String> values;

        private MapResourceBundle(Map<String, String> values) {
            this.values = values;
        }

        @Override
        protected Object handleGetObject(String key) {
            return values.get(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.enumeration(values.keySet());
        }
    }
}
