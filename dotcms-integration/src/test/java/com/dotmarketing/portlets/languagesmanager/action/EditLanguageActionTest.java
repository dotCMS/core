package com.dotmarketing.portlets.languagesmanager.action;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotLanguageException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import org.junit.BeforeClass;
import org.junit.Test;

public class EditLanguageActionTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link LanguageAPI#saveLanguage(Language)}
     * Given Scenario: Create the italian language twice, second time would be rejected by exception
     * ExpectedResult: Reject the second repeated language
     *
     */
    @Test(expected = DotLanguageException.class)
    public void save_existing_language_throw_exception() {

        final Language language = new Language();
        language.setLanguageCode("it");
        language.setLanguage("IT");
        language.setCountryCode("IT");
        language.setCountry("IT");
        new EditLanguageAction().saveLanguage(language);
        language.setId(0);//resets the id to save a new lang with the same properties.
        new EditLanguageAction().saveLanguage(language);
    }

    /**
     * Method to test: {@link LanguageAPI#saveLanguage(Language)}
     * Given Scenario: Create the italian language twice, and without doing any change saved in again
     * ExpectedResult: Saved successfully because we are editing a lang
     *
     */
    @Test
    public void save_existing_language_success() {

        final Language language = new Language();
        language.setLanguageCode("fr");
        language.setLanguage("fr");
        language.setCountryCode("france");
        language.setCountry("fr");
        new EditLanguageAction().saveLanguage(language);

        new EditLanguageAction().saveLanguage(language);
    }
}
