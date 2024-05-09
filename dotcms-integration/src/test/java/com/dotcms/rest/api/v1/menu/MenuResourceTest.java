package com.dotcms.rest.api.v1.menu;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import static com.dotcms.integrationtestutil.content.ContentUtils.createTestKeyValueContent;
import static com.dotcms.integrationtestutil.content.ContentUtils.deleteContentlets;

public class MenuResourceTest extends IntegrationTestBase {

    private static ContentType languageVariableContentType;
    private static User systemUser;

    private static Language englishLanguage;
    private static Language spanishLanguage;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment

        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        final String contentTypeVelocityVarName = LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME;
        try {
            // Using the provided Language Variable Content Type
            languageVariableContentType = APILocator.getContentTypeAPI(systemUser).find(contentTypeVelocityVarName);
        } catch (Exception e) {

            // Content Type not found, then create it
            final String contentTypeName = "Language Variable";
            final Host site = APILocator.getHostAPI().findDefaultHost(systemUser, Boolean.FALSE);

            languageVariableContentType = new ContentTypeDataGen()
                    .baseContentType(BaseContentType.KEY_VALUE)
                    .host(site)
                    .description("Testing the Language Variable API.")
                    .name(contentTypeName)
                    .velocityVarName(contentTypeVelocityVarName)
                    .fixed(Boolean.FALSE)
                    .user(systemUser).nextPersisted();
        }
        Assert.assertNotNull("The Language Variable Content Type MUST EXIST in order to run this Integration Test.",
                languageVariableContentType);

        //Search for the default language
        englishLanguage = APILocator.getLanguageAPI().getDefaultLanguage();

        //Search for the Spanish language, if does not exist we need to create it
        spanishLanguage = APILocator.getLanguageAPI().getLanguage("es", "ES");
        if (spanishLanguage == null || spanishLanguage.getId() < 1) {
            spanishLanguage = new LanguageDataGen()
                    .country("Spain")
                    .countryCode("ES")
                    .languageCode("es")
                    .languageName("Spanish").nextPersisted();
        }
    }

    @AfterClass
    public static void cleanUp() {
        LanguageDataGen.remove(spanishLanguage);
        ContentTypeDataGen.remove(languageVariableContentType);
    }

    /**
     * Method to test: {@link MenuResource#getTranslation(User, String)}
     * Given Scenario: Creates a language variable to translate Dev tools as a dev.tools (herramientas de desarrollo)
     * ExpectedResult: Should retrieve the dev.tools in spanish.
     */
    @Test
    public void test_get_translation() throws Exception {

        final String greatDevTool = "Great Developer Tools";
        final Contentlet contentletLangVar = createTestKeyValueContent(
                "dev.tools", greatDevTool, englishLanguage.getId(),
                languageVariableContentType, systemUser);
        APILocator.getLanguageAPI().saveLanguageKeys(englishLanguage,
                Map.of("dev.tools", greatDevTool), Collections.emptyMap(), Collections.emptySet());
        try {

            Assert.assertTrue(
                    "Failed creating a new Contentlet using the Language Variable Content Type.",
                    UtilMethods.isSet(contentletLangVar.getIdentifier()));

            User mainUser = new User();
            mainUser.setCompanyId(PublicCompanyFactory.getDefaultCompanyId());
            mainUser.setLocale(new Locale("en","US"));
            final String devToolTranslation = new MenuResource().getTranslation(mainUser, "Dev Tools");

            Assert.assertEquals("The string translated should be '" +
                    greatDevTool + "'", greatDevTool, devToolTranslation);
        } finally {
            //Clean up
            if (null != contentletLangVar) {
                deleteContentlets(systemUser, contentletLangVar);
            }
        }
    }

}
