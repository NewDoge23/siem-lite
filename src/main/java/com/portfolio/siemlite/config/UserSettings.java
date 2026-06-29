package com.portfolio.siemlite.config;

import com.portfolio.siemlite.localization.LanguageOption;

public record UserSettings(LanguageOption language) {

    public UserSettings {
        if (language == null) {
            language = LanguageOption.defaultLanguage();
        }
    }

    public static UserSettings defaults() {
        return new UserSettings(LanguageOption.defaultLanguage());
    }
}
