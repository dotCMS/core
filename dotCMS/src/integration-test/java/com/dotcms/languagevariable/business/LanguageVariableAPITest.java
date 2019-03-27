package com.dotcms.languagevariable.business;

import static com.dotcms.contenttype.model.type.KeyValueContentType.MULTILINGUABLE_FALLBACK_KEY;
import static com.dotcms.integrationtestutil.content.ContentUtils.createTestKeyValueContent;
import static com.dotcms.integrationtestutil.content.ContentUtils.deleteContentlets;

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
import com.liferay.util.SystemProperties;
import java.util.Date;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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

    static {
        SystemProperties.getProperties();
    }

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment

        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        final String contentTypeVelocityVarName = LanguageVariableAPI.LANGUAGEVARIABLE;
        try {
            // Using the provided Language Variable Content Type
            languageVariableContentType = APILocator.getContentTypeAPI(systemUser).find(contentTypeVelocityVarName);
        } catch (Exception e) {
            // Content Type not found, then create it
            final String contentTypeName = "Language Variable";
            systemUser = APILocator.systemUser();
            final Host site = APILocator.getHostAPI().findDefaultHost(systemUser, Boolean.FALSE);
            ContentTypeAPI contentTypeApi = APILocator.getContentTypeAPI(systemUser);
            languageVariableContentType = ContentTypeBuilder.builder(KeyValueContentType.class).host(site.getIdentifier())
                    .description("Testing the Language Variable API.").name(contentTypeName)
                    .variable(contentTypeVelocityVarName).fixed(Boolean.FALSE)
                    .owner(systemUser.getUserId()).build();
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

        final Contentlet contentletEnglish = createTestKeyValueContent(
                KEY_1 + new Date().getTime(), VALUE_1, englishLanguageId,
                languageVariableContentType, systemUser);
        final Contentlet contentletSpanish = createTestKeyValueContent(
                KEY_1 + new Date().getTime(), VALUE_1, spanishLanguageId,
                languageVariableContentType, systemUser);

        try {
            Assert.assertTrue(
                    "Failed creating a new english Contentlet using the Language Variable Content Type.",
                    UtilMethods.isSet(contentletEnglish.getIdentifier()));
            Assert.assertTrue(
                    "Failed creating a new spanish Contentlet using the Language Variable Content Type.",
                    UtilMethods.isSet(contentletSpanish.getIdentifier()));
        } finally {
            //Clean up
            if (null != contentletEnglish) {
                deleteContentlets(systemUser, contentletEnglish);
            }
            if (null != contentletSpanish) {
                deleteContentlets(systemUser, contentletSpanish);
            }
        }
    }

    /*
     * Retrieving an english Language Variable correctly.
     */
    @Test
    public void getLanguageVariable() throws DotContentletValidationException, DotContentletStateException,
            IllegalArgumentException, DotDataException, DotSecurityException {
        final LanguageVariableAPI languageVariableAPI = APILocator.getLanguageVariableAPI();

        String key = KEY_1 + new Date().getTime();

        final Contentlet contentlet = createTestKeyValueContent(key, VALUE_1,
                englishLanguageId,
                languageVariableContentType, systemUser);

        try {
            Assert.assertTrue(
                    "Failed creating a new Contentlet using the Language Variable Content Type.",
                    UtilMethods.isSet(contentlet.getIdentifier()));

            final String languageVariable = languageVariableAPI
                    .getLanguageVariable(key, englishLanguageId, systemUser);

            Assert.assertTrue("Language Variable CANNOT BE null/empty.",
                    UtilMethods.isSet(languageVariable));
            Assert.assertNotEquals(key, languageVariable);
            Assert.assertEquals(VALUE_1, languageVariable);
        } finally {
            //Clean up
            if (null != contentlet) {
                deleteContentlets(systemUser, contentlet);
            }
        }
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

        createPortugueseLanguageWithCountry();
        createPortugueseLanguageNoCountry();
        final Language portugueseLanguage = languageAPI
                .getLanguage(PORTUGAL_LANGUAGE_CODE, PORTUGAL_COUNTRY_CODE);
        final Language fallbackPortugueseLanguage = languageAPI
                .getFallbackLanguage(PORTUGAL_LANGUAGE_CODE);

        Assert.assertNotNull(portugueseLanguage);
        Assert.assertTrue(portugueseLanguage.getId() > 0);
        Assert.assertNotNull(fallbackPortugueseLanguage);
        Assert.assertTrue(fallbackPortugueseLanguage.getId() > 0);

        String key = KEY_1 + new Date().getTime();

        final Contentlet fallbackContentlet = createTestKeyValueContent(key, VALUE_1,
                fallbackPortugueseLanguage.getId(), languageVariableContentType, systemUser);

        try {
            Assert.assertTrue(
                    "Failed creating a fallback portuguese Contentlet using the Language Variable Content Type.",
                    UtilMethods.isSet(fallbackContentlet.getIdentifier()));

            final String languageVariable = languageVariableAPI
                    .getLanguageVariable(key, portugueseLanguage.getId(), systemUser);

            Assert.assertNotNull(languageVariable);
            Assert.assertNotEquals(key, languageVariable);
            Assert.assertEquals(VALUE_1, languageVariable);
        } finally {
            //Clean up
            if (null != fallbackContentlet) {
                deleteContentlets(systemUser, fallbackContentlet);
            }
        }
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

        String key = KEY_1 + new Date().getTime();

        final Contentlet contentlet = createTestKeyValueContent(key, VALUE_1, englishLanguageId,
                languageVariableContentType, systemUser);

        try {
            Assert.assertTrue(
                    "Failed creating a new Contentlet using the Language Variable Content Type.",
                    UtilMethods.isSet(contentlet.getIdentifier()));

            Config.setProperty(MULTILINGUABLE_FALLBACK_KEY, Boolean.FALSE);
            String languageVariable = languageVariableAPI
                    .getLanguageVariable(key, spanishLanguageId, systemUser);

            Assert.assertNotNull(languageVariable);
            Assert.assertEquals(key, languageVariable);

            Config.setProperty(MULTILINGUABLE_FALLBACK_KEY, Boolean.TRUE);
            languageVariable = languageVariableAPI
                    .getLanguageVariable(key, spanishLanguageId, systemUser);

            Assert.assertNotNull(languageVariable);
            Assert.assertNotEquals(key, languageVariable);
            Assert.assertEquals(VALUE_1, languageVariable);
        } finally {
            //Clean up
            Config.setProperty(MULTILINGUABLE_FALLBACK_KEY, Boolean.FALSE);
            if (null != contentlet) {
                deleteContentlets(systemUser, contentlet);
            }
        }

    }

    private static void createPortugueseLanguageWithCountry() {

        Language language;

        try {
            language =
                    APILocator.getLanguageAPI()
                            .getLanguage(PORTUGAL_LANGUAGE_CODE, PORTUGAL_COUNTRY_CODE);
        } catch (Exception e) {

            language = null;
        }

        if (null == language) {
            language = new Language();
            language.setLanguageCode(PORTUGAL_LANGUAGE_CODE);
            language.setCountryCode(PORTUGAL_COUNTRY_CODE);
            language.setLanguage(PORTUGAL_LANGUAGE_NAME);
            language.setCountry(PORTUGAL_COUNTRY_NAME);
            APILocator.getLanguageAPI().saveLanguage(language);
        }
    }

    private static void createPortugueseLanguageNoCountry() {

        Language language;

        try {
            language =
                    APILocator.getLanguageAPI().getLanguage(PORTUGAL_LANGUAGE_CODE, "");
        } catch (Exception e) {

            language = null;
        }

        if (null == language) {
            language = new Language();
            language.setLanguageCode(PORTUGAL_LANGUAGE_CODE);
            language.setLanguage(PORTUGAL_LANGUAGE_NAME);
            APILocator.getLanguageAPI().saveLanguage(language);
        }
    }

}