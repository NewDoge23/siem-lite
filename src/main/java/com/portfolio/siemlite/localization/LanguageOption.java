package com.portfolio.siemlite.localization;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum LanguageOption {

    ENGLISH("en", "English", Locale.ENGLISH),
    SPANISH("es", "Español", Locale.forLanguageTag("es"));

    private final String code;
    private final String displayName;
    private final Locale locale;

    LanguageOption(String code, String displayName, Locale locale) {
        this.code = code;
        this.displayName = displayName;
        this.locale = locale;
    }

    public String getCode() {
        return code;
    }

    public Locale getLocale() {
        return locale;
    }

    public static LanguageOption defaultLanguage() {
        return ENGLISH;
    }

    public static LanguageOption fromCode(String code) {
        if (code == null || code.isBlank()) {
            return defaultLanguage();
        }

        return Arrays.stream(values())
                .filter(language -> language.code.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElse(defaultLanguage());
    }

    public static List<LanguageOption> supportedLanguages() {
        return List.of(values());
    }

    @Override
    public String toString() {
        return displayName;
    }
}
