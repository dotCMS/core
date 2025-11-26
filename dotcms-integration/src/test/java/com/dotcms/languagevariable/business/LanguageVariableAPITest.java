package com.dotcms.languagevariable.business;

import static com.dotcms.contenttype.model.type.KeyValueContentType.MULTILINGUABLE_FALLBACK_KEY;
import static com.dotcms.integrationtestutil.content.ContentUtils.createTestKeyValueContent;
import static com.dotcms.integrationtestutil.content.ContentUtils.deleteContentlets;
import static com.dotcms.integrationtestutil.content.ContentUtils.updateTestKeyValueContent;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.LanguageVariableDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageCache;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.SystemProperties;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.AfterClass;
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

    private static Language englishLanguage;
    private static Language spanishLanguage;

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

    /*
     * Saving a Language Variable in english and spanish.
     */
    @Test
    public void saveLanguageVariableContent() throws DotContentletValidationException, DotContentletStateException,
            IllegalArgumentException, DotDataException, DotSecurityException {

        final Contentlet contentletEnglish = createTestKeyValueContent(
                KEY_1 + new Date().getTime(), VALUE_1, englishLanguage.getId(),
                languageVariableContentType, systemUser);
        final Contentlet contentletSpanish = createTestKeyValueContent(
                KEY_1 + new Date().getTime(), VALUE_1, spanishLanguage.getId(),
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
                englishLanguage.getId(),
                languageVariableContentType, systemUser);

        try {
            Assert.assertTrue(
                    "Failed creating a new Contentlet using the Language Variable Content Type.",
                    UtilMethods.isSet(contentlet.getIdentifier()));

            final String languageVariable = languageVariableAPI
                    .getLanguageVariable(key, englishLanguage.getId(), systemUser);

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

        final Contentlet contentlet = createTestKeyValueContent(key, VALUE_1,
                englishLanguage.getId(),
                languageVariableContentType, systemUser);

        try {
            Assert.assertTrue(
                    "Failed creating a new Contentlet using the Language Variable Content Type.",
                    UtilMethods.isSet(contentlet.getIdentifier()));

            Config.setProperty(MULTILINGUABLE_FALLBACK_KEY, Boolean.FALSE);
            String languageVariable = languageVariableAPI
                    .getLanguageVariable(key, spanishLanguage.getId(), systemUser);

            Assert.assertNotNull(languageVariable);
            Assert.assertEquals(key, languageVariable);

            Config.setProperty(MULTILINGUABLE_FALLBACK_KEY, Boolean.TRUE);
            languageVariable = languageVariableAPI
                    .getLanguageVariable(key, spanishLanguage.getId(), systemUser);

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

    @Test
    public void getLanguageVariableLiveVersion() throws DotContentletValidationException,
            DotContentletStateException, IllegalArgumentException, DotDataException, DotSecurityException {

        final LanguageVariableAPI languageVariableAPI = APILocator.getLanguageVariableAPI();

        String key = KEY_1 + new Date().getTime();

        final Contentlet contentlet = createTestKeyValueContent(key, VALUE_1,
                englishLanguage.getId(),
                languageVariableContentType, systemUser);

        try {

            String languageVariable = languageVariableAPI
                    .getLanguageVariable(key, englishLanguage.getId(), systemUser, true, false);

            Assert.assertNotNull(languageVariable);
            Assert.assertNotEquals(key, languageVariable);
            Assert.assertEquals(VALUE_1, languageVariable);
        } finally {
            if (null != contentlet) {
                deleteContentlets(systemUser, contentlet);
            }
        }

    }

    @Test
    public void getLanguageVariableWorkingVersion()
            throws DotContentletStateException, IllegalArgumentException, DotDataException, DotSecurityException, InterruptedException {

        final String newValue = "NEW_VALUE";
        final LanguageVariableAPI languageVariableAPI = APILocator.getLanguageVariableAPI();

        String key = KEY_1 + new Date().getTime();

        final Contentlet contentlet = createTestKeyValueContent(key, VALUE_1,
                englishLanguage.getId(),
                languageVariableContentType, systemUser);

        updateTestKeyValueContent(contentlet, key, newValue, englishLanguage.getId(), languageVariableContentType, systemUser);
        Thread.sleep(1000);

        try {

            String languageVariable = languageVariableAPI
                    .getLanguageVariable(key, englishLanguage.getId(), systemUser, false, false);

            Assert.assertNotNull(languageVariable);
            Assert.assertNotEquals(key, languageVariable);
            Assert.assertEquals(newValue, languageVariable);
        } finally {
            //Clean up
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

    /**
     * Given scenario: Create 3 languages and 3 language variables for each language and then unpublish all the content
     * Expected Result: 1. The API should return all the language variables created regardless of status (published or unpublished)
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void findLanguageVariablesThenUnpublishTest() throws Exception {
        
        destroyAll();
        final LanguageCache languageCache = CacheLocator.getLanguageCache();
        languageCache.clearVariables();
        final List<LanguageVariable> vars = languageCache.getVars(1);
        Assert.assertTrue(vars.isEmpty());
        //Let's create 3 languages and 3 language variables for each language
        final List<Language> languages = List.of(
                new LanguageDataGen().nextPersisted(),
                new LanguageDataGen().nextPersisted(),
                new LanguageDataGen().nextPersisted()
        );

        List<Contentlet> contentlets = new ArrayList<>();
        LanguageVariableDataGen languageVariableDataGen = new LanguageVariableDataGen();
        //Contentlets are created and published
        for (Language language : languages) {
            contentlets.add(languageVariableDataGen.languageId(language.getId()).key("key1").value("value1").nextPersistedAndPublish());
            contentlets.add(languageVariableDataGen.languageId(language.getId()).key("key2").value("value2").nextPersistedAndPublish());
            contentlets.add(languageVariableDataGen.languageId(language.getId()).key("key3").value("value3").nextPersistedAndPublish());
        }

        final LanguageVariableAPI languageVariableAPI = APILocator.getLanguageVariableAPI();
        //Now let's see if the API can find all the variables
        for (Language language:languages) {
            final List<LanguageVariable> languageVariables = languageVariableAPI.findVariables(language.getId());
            Assert.assertTrue(languageVariables.size() >= 3);
            for (LanguageVariable variable : languageVariables) {
                Assert.assertTrue(variable.key().startsWith("key"));
                Assert.assertTrue(variable.value().startsWith("value"));
                Assert.assertTrue(containsIdentifier(contentlets, variable.identifier()));
            }
        }
        //Now let's unpublish all the content
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        for (Contentlet contentlet: contentlets) {
            contentletAPI.unpublish(contentlet, systemUser,false);
        }
        // Now let's see if the API can find all the variables regardless of status
        final Map<Language, List<LanguageVariable>> allVariables = languageVariableAPI.findAllVariables();
        Assert.assertFalse(allVariables.isEmpty());

        allVariables.forEach((language, languageVariables) -> {
            for (LanguageVariable variable : languageVariables) {
                Assert.assertTrue("Unpublished Variables should still appear in the contentlets first created",
                        containsIdentifier(contentlets, variable.identifier()));
            }
        });
    }

    /**
     * Method to test {@link com.dotmarketing.portlets.languagesmanager.business.LanguageCacheImpl#ifPresentGetOrElseFetch(long, Callable)} ()} method.
     * Given scenario: We create LangVariables data specific for new languages then we play with the cache
     * Expected Result: The fetch function should only be called when the cache is empty
     * @throws DotDataException
     */
    @Test
    public void testCachedFindVariables() throws DotDataException {
        final LanguageCache languageCache = CacheLocator.getLanguageCache();
        languageCache.clearVariables();

        //Let's create 3 languages and 3 language variables for each language
        final List<Language> languages = List.of(
                new LanguageDataGen().nextPersisted(),
                new LanguageDataGen().nextPersisted(),
                new LanguageDataGen().nextPersisted()
        );

        List<Contentlet> contentlets = new ArrayList<>();
        LanguageVariableDataGen languageVariableDataGen = new LanguageVariableDataGen();
        //Contentlets are created and published
        for (Language language : languages) {
            contentlets.add(languageVariableDataGen.languageId(language.getId()).key("cachedKey1").value("cachedValue1").nextPersistedAndPublish());
            contentlets.add(languageVariableDataGen.languageId(language.getId()).key("cachedKey2").value("cachedValue2").nextPersistedAndPublish());
            contentlets.add(languageVariableDataGen.languageId(language.getId()).key("cachedKey3").value("cachedValue3").nextPersistedAndPublish());
        }
        //Select any rand language to test the cache
        final long id = languages.stream().findAny().get().getId();
        //Even though the contentlets are created, the cache should be empty
        Assert.assertTrue(languageCache.getVars(id).isEmpty());
        final List<LanguageVariable> cached = languageCache.ifPresentGetOrElseFetch(id,
                () -> List.of(
                        ImmutableLanguageVariable.builder().key("mockKey").value("mockValue")
                                .identifier("mockIdentifier").build()
                ));

        //The cache should have been populated with the mock variables
        Assert.assertFalse(cached.isEmpty());
        Assert.assertEquals(1, cached.size());
        Assert.assertEquals("mockKey", cached.get(0).key());

        //Now let's see if the API can find all the variables
        final LanguageVariableAPI languageVariableAPI = APILocator.getLanguageVariableAPI();
        final List<LanguageVariable> languageVariables = languageVariableAPI.findVariables(id);
        Assert.assertEquals(1, languageVariables.size());

        Assert.assertEquals("mockKey", languageVariables.get(0).key());

        // now lets test the cache after calling the API
        languageCache.clearVariables();
        final Map<Language, List<LanguageVariable>> allVariables = languageVariableAPI.findAllVariables();
        final AtomicBoolean fetchCalled = new AtomicBoolean(false);
        for (final Language language : languages) {
            final List<LanguageVariable> variables = languageCache.ifPresentGetOrElseFetch(language.getId(),
                    () -> {
                        fetchCalled.set(true);
                        return List.of();
                    });
            // there should always be 3 variables per language and the fetch should have never been called.
            // That because the variables are already in the cache
            Assert.assertFalse(variables.isEmpty());
            //test we're getting the right variables
            Assert.assertEquals(3, variables.size());
            //Test that the fetch was never called
            Assert.assertFalse(fetchCalled.get());
        }

        //Finally test the cache with a language that does not exist
        final List<LanguageVariable> nonExistingLang = languageCache.ifPresentGetOrElseFetch(0,
                List::of);

        Assert.assertTrue(nonExistingLang.isEmpty());

    }

    /**
     * helper method to check if a list of contentlets contains a contentlet with a given inode
     * @param variables list of contentlets
     * @param identifier  the inode to check for
     * @return  true if the inode is found in the list of contentlets
     */
    boolean containsIdentifier(final List<Contentlet> variables, final String identifier) {
        return variables.stream().anyMatch(variable -> variable.getIdentifier().equals(identifier));
    }


    public static void destroyAll() {
        final User sysUser = APILocator.systemUser();
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final LanguageVariableAPI languageVariableAPI = APILocator.getLanguageVariableAPI();
        final Map<Language, List<LanguageVariable>> allVariables = languageVariableAPI.findAllVariables();
        allVariables.entrySet().stream().flatMap(entry -> entry.getValue().stream())
                .forEach(languageVariable -> {
                    try {
                        final List<Contentlet> allVersions = contentletAPI.findAllVersions(new Identifier(languageVariable.identifier()), sysUser, false);
                        allVersions.forEach(cont -> {
                            try {
                                contentletAPI.destroy(cont, sysUser, false);
                            } catch (DotDataException | DotStateException | DotSecurityException e) {
                                Logger.error(LanguageVariableAPITest.class, e.getMessage(), e);
                            }
                        });
                    } catch (DotDataException | DotStateException | DotSecurityException e) {
                        Logger.error(LanguageVariableAPITest.class, e.getMessage(), e);
                    }
                });
        final LanguageCache languageCache = CacheLocator.getLanguageCache();
        languageCache.clearVariables();
    }
}