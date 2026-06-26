package com.portfolio.siemlite.localization;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageOptionTest {

    @Test
    void resolvesSupportedLanguageByCode() {
        assertEquals(LanguageOption.SPANISH, LanguageOption.fromCode("es"));
    }

    @Test
    void fallsBackToEnglishForUnknownCode() {
        assertEquals(LanguageOption.ENGLISH, LanguageOption.fromCode("unknown"));
    }

    @Test
    void exposesLocaleForSupportedLanguage() {
        assertEquals(Locale.ENGLISH, LanguageOption.ENGLISH.getLocale());
        assertEquals(Locale.forLanguageTag("es"), LanguageOption.SPANISH.getLocale());
    }
}
