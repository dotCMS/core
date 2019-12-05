package com.dotcms.mock.request;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class CachedParameterDecoratorTest extends IntegrationTestBase {


    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testDecorate() throws Exception {

        final Language baseLanguage  = new LanguageDataGen().languageCode("it").countryCode(null).nextPersisted();
        final Language itLanguage    = new LanguageDataGen().languageCode("it").countryCode("IT").nextPersisted();
        final long expectedBaseId  = baseLanguage.getId();
        final long expectedItalyId = itLanguage.getId();
        final LanguageIdParameterDecorator languageIdParameterDecorator = new LanguageIdParameterDecorator();

        final long languageId1  = Long.parseLong(new CachedParameterDecorator(languageIdParameterDecorator).decorate("it_IT"));
        final long languageId2  = Long.parseLong(new CachedParameterDecorator(languageIdParameterDecorator).decorate("it"));

        Assert.assertEquals(expectedItalyId, languageId1);
        Assert.assertEquals(expectedBaseId,  languageId2);

        APILocator.getLanguageAPI().deleteLanguage(baseLanguage);
        APILocator.getLanguageAPI().deleteLanguage(itLanguage);
    }
}
