package com.dotmarketing.portlets.languagesmanager.action;

import com.dotmarketing.exception.DotLanguageException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import org.junit.Test;

public class EditLanguageActionTest {

    /**
     * Method to test: {@link LanguageAPI#saveLanguage(Language)}
     * Given Scenario: Create the italy languages twice, second time would be rejected by exception
     * ExpectedResult: Reject the second repeated language
     *
     */
    @Test(expected = DotLanguageException.class)
    public void save_existing_language_throw_exception() throws Exception {

        final Language language = new Language();
        language.setLanguageCode("it");
        language.setLanguage("IT");
        language.setCountryCode("IT");
        language.setCountry("IT");
        new EditLanguageAction().saveLanguage(language);

        new EditLanguageAction().saveLanguage(language);
    }
}
