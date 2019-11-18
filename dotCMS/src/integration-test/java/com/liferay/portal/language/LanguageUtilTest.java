package com.liferay.portal.language;

import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.liferay.portal.SystemException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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

    @Test
    public void getLanguageId_existing_base_expected_non_negative_lang_Test() throws DotSecurityException, DotDataException, SystemException {

        final Language baseLanguage = new LanguageDataGen().languageCode("fr").countryCode(null).nextPersisted();
        final Language frLanguage   = new LanguageDataGen().languageCode("fr").countryCode("FR").nextPersisted();
        final long expectedBaseId   = baseLanguage.getId();
        final long expectedFrId     = frLanguage.getId();
        Logger.info(this, "Testing fr_CR");
        Assert.assertEquals(expectedBaseId, LanguageUtil.getLanguageId("fr_CR"));
        Logger.info(this, "Testing fr-CR");
        Assert.assertEquals(expectedBaseId, LanguageUtil.getLanguageId("fr-CR"));
        Logger.info(this, "Testing fr_FR");
        Assert.assertEquals(expectedFrId,   LanguageUtil.getLanguageId("fr_FR"));
        Logger.info(this, "Testing FR_FR");
        Assert.assertEquals(expectedFrId,   LanguageUtil.getLanguageId("FR_FR"));
        Logger.info(this, "Testing fr-FR");
        Assert.assertEquals(expectedFrId,   LanguageUtil.getLanguageId("fr-FR"));
        Logger.info(this, "Testing FR-FR");
        Assert.assertEquals(expectedFrId,   LanguageUtil.getLanguageId("FR-FR"));
        Logger.info(this, "Testing fr");
        Assert.assertEquals(expectedBaseId,   LanguageUtil.getLanguageId("fr"));
        Logger.info(this, "Testing FR");
        Assert.assertEquals(expectedBaseId,   LanguageUtil.getLanguageId("FR"));
        APILocator.getLanguageAPI().deleteLanguage(baseLanguage);
        APILocator.getLanguageAPI().deleteLanguage(frLanguage);
    }

}
