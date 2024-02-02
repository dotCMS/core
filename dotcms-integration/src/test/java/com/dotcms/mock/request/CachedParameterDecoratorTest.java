package com.dotcms.mock.request;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;
import java.util.Random;

public class CachedParameterDecoratorTest extends IntegrationTestBase {


    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testDecorate() {
        final String   languageCode  = "mylc";

        final Language baseLanguage  = new LanguageDataGen().languageCode(languageCode).countryCode(null).nextPersisted();
        final Language itLanguage    = new LanguageDataGen().languageCode(languageCode).countryCode("IT").nextPersisted();
        final long expectedBaseId  = baseLanguage.getId();
        final long expectedItalyId = itLanguage.getId();
        final LanguageIdParameterDecorator languageIdParameterDecorator = new LanguageIdParameterDecorator();

        final long languageId1  = Long.parseLong(new CachedParameterDecorator(languageIdParameterDecorator).decorate(languageCode + "_IT"));
        final long languageId2  = Long.parseLong(new CachedParameterDecorator(languageIdParameterDecorator).decorate(languageCode));

        Assert.assertEquals(expectedItalyId, languageId1);
        Assert.assertEquals(expectedBaseId,  languageId2);

    }
}
