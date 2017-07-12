package com.dotcms.languagevariable.business;

import static com.dotcms.integrationtestutil.content.ContentUtils.createTestLanguageVariableContent;
import static com.dotcms.integrationtestutil.content.ContentUtils.deleteContentlets;

import com.liferay.util.SystemProperties;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * This Integration Test will verify the correct and expected behavior of the
 * {@link LanguageVariableAPI}.
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 29, 2017
 *
 */
public class LanguageVariableAPITest extends IntegrationTestBase {

    private static ContentType languageVariableContentType;
    private static User systemUser;

    private static long englishLanguageId;
    private static long spanishLanguageId;

    private static final String KEY_1 = "com.dotcms.languagevariable.variable1";
    private static final String VALUE_1 = "Test Language Variable #1";
    private static final String PORTUGAL_LANGUAGE_CODE = "pt";
    private static final String PORTUGAL_LANGUAGE_NAME = "Portuguese";
    private static final String PORTUGAL_COUNTRY_CODE = "pt";
    private static final String PORTUGAL_COUNTRY_NAME = "Portugal";
    private static final String DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE = "DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE";

    static {
        SystemProperties.getProperties();
    }

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment

        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        try {
            // Using the provided Language Variable Content Type
            languageVariableContentType = APILocator.getContentTypeAPI(systemUser).find("Languagevariable");
        } catch (Exception e) {
            // Content Type not found, then create it
            final String contentTypeName = "Language Variable";
            final String contentTypeVelocityVarName = "Languagevariable";
            systemUser = APILocator.systemUser();
            final Host site = APILocator.getHostAPI().findDefaultHost(systemUser, Boolean.FALSE);
            ContentTypeAPI contentTypeApi = APILocator.getContentTypeAPI(systemUser);
            languageVariableContentType = ContentTypeBuilder.builder(KeyValueContentType.class).host(site.getIdentifier())
                            .description("Testing the Language Variable API.").name(contentTypeName)
                            .variable(contentTypeVelocityVarName).fixed(Boolean.FALSE).owner(systemUser.getUserId()).build();
            languageVariableContentType = contentTypeApi.save(languageVariableContentType);
        }
        Assert.assertNotNull("The Language Variable Content Type MUST EXIST in order to run this Integration Test.",
                        languageVariableContentType);
        englishLanguageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        spanishLanguageId = APILocator.getLanguageAPI().getLanguage("es", "ES").getId();
    }

    /*
     * Saving a Language Variable in english and spanish.
     */
    @Test
    public void saveLanguageVariableContent() throws DotContentletValidationException, DotContentletStateException,
                    IllegalArgumentException, DotDataException, DotSecurityException {
        final Contentlet contentletEnglish = createTestLanguageVariableContent(KEY_1, VALUE_1, englishLanguageId,
                        languageVariableContentType, systemUser);
        final Contentlet contentletSpanish = createTestLanguageVariableContent(KEY_1, VALUE_1, spanishLanguageId,
                        languageVariableContentType, systemUser);

        Assert.assertTrue("Failed creating a new english Contentlet using the Language Variable Content Type.",
                        UtilMethods.isSet(contentletEnglish.getIdentifier()));
        Assert.assertTrue("Failed creating a new spanish Contentlet using the Language Variable Content Type.",
                        UtilMethods.isSet(contentletSpanish.getIdentifier()));

        deleteContentlets(systemUser, contentletEnglish, contentletSpanish);
    }

    /*
     * Retrieving an english Language Variable correctly.
     */
    @Test
    public void getLanguageVariable() throws DotContentletValidationException, DotContentletStateException,
                    IllegalArgumentException, DotDataException, DotSecurityException {
        final LanguageVariableAPI languageVariableAPI = APILocator.getLanguageVariableAPI();
        final Contentlet contentlet = createTestLanguageVariableContent(KEY_1, VALUE_1, englishLanguageId,
                        languageVariableContentType, systemUser);

        Assert.assertTrue("Failed creating a new Contentlet using the Language Variable Content Type.",
                        UtilMethods.isSet(contentlet.getIdentifier()));

        final String languageVariable = languageVariableAPI.getLanguageVariable(KEY_1, englishLanguageId, systemUser);

        Assert.assertTrue("Language Variable CANNOT BE null/empty.", UtilMethods.isSet(languageVariable));

        deleteContentlets(systemUser, contentlet);
    }

    /*
     * Testing the fallback mechanism which means that, for example, if looking for a Language
     * Variable associated to "en_US" and it doesn't exist, then look for a Language Variable
     * associated to "en" (only the language).
     */
    @Test
    public void getLanguageVariableUsingFallbackLanguage() throws DotContentletValidationException, DotContentletStateException,
                    IllegalArgumentException, DotDataException, DotSecurityException {
        final LanguageVariableAPI languageVariableAPI = APILocator.getLanguageVariableAPI();
        final LanguageAPI languageAPI = APILocator.getLanguageAPI();
        Language portugueseLanguage = new Language();
        portugueseLanguage.setLanguageCode(PORTUGAL_LANGUAGE_CODE);
        portugueseLanguage.setCountryCode(PORTUGAL_COUNTRY_CODE);
        portugueseLanguage.setLanguage(PORTUGAL_LANGUAGE_NAME);
        portugueseLanguage.setCountry(PORTUGAL_COUNTRY_NAME);
        Language fallbackPortugueseLanguage = new Language();
        fallbackPortugueseLanguage.setLanguageCode(PORTUGAL_LANGUAGE_CODE);
        fallbackPortugueseLanguage.setLanguage(PORTUGAL_LANGUAGE_NAME);
        languageAPI.saveLanguage(portugueseLanguage);
        languageAPI.saveLanguage(fallbackPortugueseLanguage);
        portugueseLanguage = languageAPI.getLanguage(PORTUGAL_LANGUAGE_CODE, PORTUGAL_COUNTRY_CODE);
        fallbackPortugueseLanguage = languageAPI.getFallbackLanguage(PORTUGAL_LANGUAGE_CODE);

        final Contentlet fallbackContentlet = createTestLanguageVariableContent(KEY_1, VALUE_1,
                        fallbackPortugueseLanguage.getId(), languageVariableContentType, systemUser);

        Assert.assertTrue("Failed creating a fallback portuguese Contentlet using the Language Variable Content Type.",
                        UtilMethods.isSet(fallbackContentlet.getIdentifier()));

        final String languageVariable = languageVariableAPI.getLanguageVariable(KEY_1, portugueseLanguage.getId(), systemUser);

        Assert.assertTrue("Language Variable CANNOT BE null/empty.", UtilMethods.isSet(languageVariable));

        deleteContentlets(systemUser, fallbackContentlet);
        languageAPI.deleteLanguage(portugueseLanguage);
        languageAPI.deleteFallbackLanguage(fallbackPortugueseLanguage);
    }

    /*
     * Testing the "MULTILINGUAGE_FALLBACK" fallback, which means that a key that
     * doesn't exist neither for a given language and country nor for a single language, will be
     * looked up using the default system language.
     */
    @Test
    public void getLanguageVariableUsingDefaultContentToDefaultLanguage() throws DotContentletValidationException,
                    DotContentletStateException, IllegalArgumentException, DotDataException, DotSecurityException {
        final LanguageVariableAPI languageVariableAPI = APILocator.getLanguageVariableAPI();
        final Contentlet contentlet = createTestLanguageVariableContent(KEY_1, VALUE_1, englishLanguageId,
                        languageVariableContentType, systemUser);

        Assert.assertTrue("Failed creating a new Contentlet using the Language Variable Content Type.",
                        UtilMethods.isSet(contentlet.getIdentifier()));

        Config.setProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, Boolean.FALSE);
        String languageVariable = languageVariableAPI.getLanguageVariable(KEY_1, spanishLanguageId, systemUser);

        Assert.assertTrue("Language Variable MUST BE null/empty.", !UtilMethods.isSet(languageVariable));

        Config.setProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, Boolean.TRUE);
        languageVariable = languageVariableAPI.getLanguageVariable(KEY_1, spanishLanguageId, systemUser);

        Assert.assertTrue("Language Variable CANNOT BE null/empty.", UtilMethods.isSet(languageVariable));

        deleteContentlets(systemUser, contentlet);
        Config.setProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, Boolean.FALSE);
    }

}
