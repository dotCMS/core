package com.dotcms.datagen;

import static com.dotmarketing.portlets.languagesmanager.business.UniqueLanguageDataGen.testLanguageExist;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;

/**
 * @author Jonathan Gamba 2019-04-04
 */
public class LanguageDataGen extends AbstractDataGen<Language> {

    private String languageCode;
    private String languageName;
    private String countryCode;
    private String country;

    private void initialize() {
        long currentTime = System.currentTimeMillis();
        languageCode = org.apache.commons.lang.RandomStringUtils.randomAlphanumeric(5);
        languageName = "testLanguage" + currentTime;
        countryCode = "testCountryCode" + currentTime;
        country = "testCountry" + currentTime;
    }

    public LanguageDataGen() {
        initialize();
    }

    @SuppressWarnings("unused")
    public LanguageDataGen languageCode(final String languageCode) {
        this.languageCode = languageCode;
        return this;
    }

    @SuppressWarnings("unused")
    public LanguageDataGen languageName(final String languageName) {
        this.languageName = languageName;
        return this;
    }

    @SuppressWarnings("unused")
    public LanguageDataGen countryCode(final String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    @SuppressWarnings("unused")
    public LanguageDataGen country(final String country) {
        this.country = country;
        return this;
    }

    @Override
    public Language next() {
        final Language language = new Language();
        language.setLanguageCode(languageCode);
        language.setLanguage(languageName);
        language.setCountryCode(countryCode);
        language.setCountry(country);

        return language;
    }

    @WrapInTransaction
    @Override
    public Language persist(final Language language) {
        final LanguageAPI languageAPI = APILocator.getLanguageAPI();
        boolean languageExists = (testLanguageExist(language.getLanguageCode(), language.getCountryCode()) > 0);
        if (languageExists) {
            return languageAPI.getLanguage(language.getLanguageCode(), language.getCountryCode());
        }
        try {
            languageAPI.saveLanguage(language);
            return language;
        } catch (Exception e) {
            throw new RuntimeException("Unable to persist Language.", e);
        }
    }

    /**
     * Creates a new {@link Language} instance and persists it in DB
     *
     * @return A new Language instance persisted in DB
     */
    @Override
    public Language nextPersisted() {
        return persist(next());
    }

    public static void remove(final Language language) {
        remove(language, true);
    }

    @WrapInTransaction
    public static void remove(final Language language, final Boolean failSilently) {

        if (null != language) {
            try {
                APILocator.getLanguageAPI().deleteLanguage(language);
            } catch (Exception e) {
                if (failSilently) {
                    Logger.error(ContentTypeDataGen.class, "Unable to delete Language.", e);
                } else {
                    throw new RuntimeException("Unable to delete Language.", e);
                }
            }
        }
    }

}