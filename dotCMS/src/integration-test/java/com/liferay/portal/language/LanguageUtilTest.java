package com.liferay.portal.language;

import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.SystemException;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Locale;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class LanguageUtilTest {


    @BeforeClass
    public static void prepare() throws Exception {

        //Setting the test user
    	IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void getLanguageId_null_expected_negative_lang_Test() throws DotSecurityException, DotDataException, SystemException {

        final long languageId = LanguageUtil.getLanguageId(null);
        Assert.assertEquals(-1, languageId);
    }

    @Test
    public void getLanguageId_empty_expected_negative_lang_Test() throws DotSecurityException, DotDataException, SystemException {

        final long languageId = LanguageUtil.getLanguageId("");
        Assert.assertEquals(-1, languageId);
    }

    @Test
    public void getLanguageId_weirdlang_expected_negative_lang_Test() throws DotSecurityException, DotDataException, SystemException {

        final long languageId = LanguageUtil.getLanguageId("owdaldlksdllakd");
        Assert.assertEquals(-1, languageId);
    }

    @Test
    public void getLanguageId_valid_default_long_expected_same_lang_id_Test() throws DotSecurityException, DotDataException, SystemException {

        final long languageId = LanguageUtil.getLanguageId("-1");
        Assert.assertEquals(-1l, languageId);
    }

    @Test
    public void getLanguageId_valid_long_expected_same_lang_id_Test() throws DotSecurityException, DotDataException, SystemException {

        final long languageId = LanguageUtil.getLanguageId("99999999999");
        Assert.assertEquals(99999999999l, languageId);
    }



    @Test
    public void getLanguageId_existing_expected_non_negative_lang_Test() throws DotSecurityException, DotDataException, SystemException {

        final Language language = new LanguageDataGen().languageCode("es").country("CR").nextPersisted();
        final long expectedId = language.getId();
        final long languageId = LanguageUtil.getLanguageId(String.valueOf(language.getId()));
        Assert.assertEquals(expectedId, languageId);
        APILocator.getLanguageAPI().deleteLanguage(language);
    }

    private static class TestCaseLanguageLocale {
        String localeRequested;
        long expectedLangId;

        TestCaseLanguageLocale(final String localeRequested,final long expectedLangId){
            this.localeRequested = localeRequested;
            this.expectedLangId = expectedLangId;
        }
    }

    @DataProvider
    public static Object[] testCasesLanguageLocale() throws Exception {

        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        final Language baseLanguage = (UtilMethods.isSet(APILocator.getLanguageAPI().getLanguage("fr",null))) ?
                APILocator.getLanguageAPI().getLanguage("fr",null) :
                new LanguageDataGen().languageCode("fr").countryCode(null).nextPersisted();
        final Language frLanguage   = (UtilMethods.isSet(APILocator.getLanguageAPI().getLanguage("fr","fr"))) ?
                APILocator.getLanguageAPI().getLanguage("fr","fr") :
                new LanguageDataGen().languageCode("fr").countryCode("FR").nextPersisted();
        final long expectedBaseLangId   = baseLanguage.getId();
        final long expectedFrLangId     = frLanguage.getId();

        return new  Object[]{
                new TestCaseLanguageLocale("fr_CR",expectedBaseLangId),
                new TestCaseLanguageLocale("fr-CR",expectedBaseLangId),
                new TestCaseLanguageLocale("fr_FR",expectedFrLangId),
                new TestCaseLanguageLocale("FR_FR",expectedFrLangId),
                new TestCaseLanguageLocale("fr-FR",expectedFrLangId),
                new TestCaseLanguageLocale("FR-FR",expectedFrLangId),
                new TestCaseLanguageLocale("FR",expectedBaseLangId),
                new TestCaseLanguageLocale("fr",expectedBaseLangId),

        };
    }

    /**
     * This test is for the getLanguageId under the LanguageUtil.
     * This method can receive a locale and get the languageId if it exists.
     * If a language with a specific CountryCode not exists but the base language does, this one should be returned.
     *
     */
    @Test
    @UseDataProvider("testCasesLanguageLocale")
    public void test_getLanguageId_DiffLanguageLocale(final TestCaseLanguageLocale testCaseLanguageLocale) {
        Assert.assertEquals(testCaseLanguageLocale.expectedLangId,LanguageUtil.getLanguageId(testCaseLanguageLocale.localeRequested));
    }

    @Test
    public void test_getAllMessagesByLocale_success(){
        final Locale locale = APILocator.getLanguageAPI().getDefaultLanguage().asLocale();
        final Map messagesMap = LanguageUtil.getAllMessagesByLocale(locale);
        Assert.assertNotNull(messagesMap);
    }

}
