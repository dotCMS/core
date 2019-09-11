package com.dotmarketing.portlets.languagesmanager.business;

import com.dotmarketing.portlets.languagesmanager.model.Language;

public class LanguageDeletedEvent  {

    private final Language language;

    public LanguageDeletedEvent(Language language) {
        this.language = language;
    }

    public Language getLanguage() {
        return language;
    }
}
