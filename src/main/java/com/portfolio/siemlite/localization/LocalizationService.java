package com.portfolio.siemlite.localization;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class LocalizationService {

    private static final String BUNDLE_BASE_NAME = "i18n.messages";
    private static final ResourceBundle.Control NO_SYSTEM_FALLBACK =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT);

    private final ResourceBundle bundle;
    private final ResourceBundle englishBundle;

    public LocalizationService(LanguageOption language) {
        this.englishBundle = loadBundle(LanguageOption.defaultLanguage());
        this.bundle = loadBundle(language == null ? LanguageOption.defaultLanguage() : language);
    }

    public String get(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }

        if (bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        if (englishBundle.containsKey(key)) {
            return englishBundle.getString(key);
        }
        return key;
    }

    public String format(String key, Object... arguments) {
        return MessageFormat.format(get(key), arguments);
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    private ResourceBundle loadBundle(LanguageOption language) {
        try {
            return ResourceBundle.getBundle(BUNDLE_BASE_NAME, language.getLocale(), NO_SYSTEM_FALLBACK);
        } catch (MissingResourceException exception) {
            return ResourceBundle.getBundle(
                    BUNDLE_BASE_NAME,
                    LanguageOption.defaultLanguage().getLocale(),
                    NO_SYSTEM_FALLBACK);
        }
    }
}
