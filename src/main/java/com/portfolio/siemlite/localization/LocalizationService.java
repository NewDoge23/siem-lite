package com.portfolio.siemlite.localization;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

public class LocalizationService {

    private static final String BUNDLE_BASE_NAME = "i18n.messages";
    private static final ResourceBundle.Control NO_SYSTEM_FALLBACK =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT);

    private final ResourceBundle bundle;
    private final ResourceBundle englishBundle;

    public LocalizationService(LanguageOption language) {
        this.englishBundle = loadBundle(LanguageOption.defaultLanguage());
        ResourceBundle selectedBundle = loadBundle(language == null ? LanguageOption.defaultLanguage() : language);
        this.bundle = new FallbackResourceBundle(selectedBundle, englishBundle);
    }

    LocalizationService(ResourceBundle selectedBundle, ResourceBundle englishBundle) {
        this.englishBundle = englishBundle;
        this.bundle = new FallbackResourceBundle(selectedBundle, englishBundle);
    }

    public String get(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }

        try {
            return bundle.getString(key);
        } catch (MissingResourceException exception) {
            return key;
        }
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

    private static final class FallbackResourceBundle extends ResourceBundle {

        private final ResourceBundle selectedBundle;
        private final ResourceBundle englishBundle;

        private FallbackResourceBundle(ResourceBundle selectedBundle, ResourceBundle englishBundle) {
            this.selectedBundle = selectedBundle;
            this.englishBundle = englishBundle;
        }

        @Override
        protected Object handleGetObject(String key) {
            try {
                return selectedBundle.getObject(key);
            } catch (MissingResourceException exception) {
                try {
                    return englishBundle.getObject(key);
                } catch (MissingResourceException fallbackException) {
                    return key;
                }
            }
        }

        @Override
        public Enumeration<String> getKeys() {
            Set<String> keys = new LinkedHashSet<>();
            selectedBundle.getKeys().asIterator().forEachRemaining(keys::add);
            englishBundle.getKeys().asIterator().forEachRemaining(keys::add);
            return Collections.enumeration(keys);
        }
    }
}
