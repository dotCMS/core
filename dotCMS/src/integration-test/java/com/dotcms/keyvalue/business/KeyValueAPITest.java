package com.dotcms.keyvalue.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.cache.KeyValueCache;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.keyvalue.model.KeyValue;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static com.dotcms.integrationtestutil.content.ContentUtils.*;

/**
 * This Integration Test will verify the correct and expected behavior of the {@link KeyValueAPI}.
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 21, 2017
 *
 */
public class KeyValueAPITest extends IntegrationTestBase {

    private static ContentType keyValueContentType;
    private static User systemUser;

    private static long englishLanguageId;
    private static long spanishLanguageId;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        // Creating a test Key/Value Content Type
        final long time = System.currentTimeMillis();
        final String contentTypeName = "Key/Value Test " + time;
        final String contentTypeVelocityVarName = "Keyvaluetest" + time;
        systemUser = APILocator.systemUser();
        final Host site = APILocator.getHostAPI().findDefaultHost(systemUser, Boolean.FALSE);
        ContentTypeAPI contentTypeApi = APILocator.getContentTypeAPI(systemUser);
        keyValueContentType = ContentTypeBuilder.builder(KeyValueContentType.class).host(site.getIdentifier())
                        .description("Testing the Key/Value API.").name(contentTypeName).variable(contentTypeVelocityVarName)
                        .fixed(Boolean.FALSE).owner(systemUser.getUserId()).build();
        keyValueContentType = contentTypeApi.save(keyValueContentType);
        englishLanguageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        spanishLanguageId = TestDataUtils.getSpanishLanguage().getId();
        setDebugMode(Boolean.FALSE);
    }

    /*
     * Creates a content version in english and in spanish with the same key/value
     * Successfully creates both versions since its the same identifier
     */
    @Test
    public void savesKeyValueContentWithVersionInEnglishAndSpanish() throws DotContentletStateException,
                    IllegalArgumentException, DotDataException, DotSecurityException {

        Contentlet contentletEnglish = null;
        Contentlet contentletSpanish = null;

        try {
            String key1 = "com.dotcms.test.key1." + new Date().getTime();
            String value1 = "Test Key #1";

            contentletEnglish = createTestKeyValueContent(key1, value1, englishLanguageId,
                    keyValueContentType,
                    systemUser);
            //Create a Spanish version of the same Contentlet
            contentletSpanish = createTestKeyValueContent(contentletEnglish.getIdentifier(), key1,
                    value1, spanishLanguageId, keyValueContentType,
                    systemUser);

            Assert.assertTrue(UtilMethods.isSet(contentletEnglish.getIdentifier()));
            Assert.assertTrue(UtilMethods.isSet(contentletSpanish.getIdentifier()));
        } finally {
            deleteContentlets(systemUser, contentletEnglish, contentletSpanish);
        }
    }

    /*
     * Tries to create 2 contents in the same language (english) with the same key/value
     * Throws DotContentletValidationException when trying to save the second one
     */
    @Test(expected = DotContentletValidationException.class)
    public void saveKeyValueContentWithUnique()
            throws DotContentletStateException,
            IllegalArgumentException, DotDataException, DotSecurityException {

        Contentlet contentlet1 = null;
        Contentlet contentlet2 = null;
        try {
            String key1 = "com.dotcms.test.key11." + new Date().getTime();
            String value1 = "Test Key #11";

            contentlet1 = createTestKeyValueContent(key1, value1, englishLanguageId,
                    keyValueContentType,
                    systemUser);
            Assert.assertTrue(UtilMethods.isSet(contentlet1.getIdentifier()));
            System.out.println("Saved!!!!!!");

            contentlet2 = createTestKeyValueContent(key1, value1, englishLanguageId,
                    keyValueContentType,
                    systemUser);
        }finally {
            deleteContentlets(systemUser, contentlet1);
        }
    }

    /*
     * Updating an existing KeyValue and verifying the cache group.
     */
    @Test
    public void updateKeyValueContent() throws Exception {

        String key1 = "com.dotcms.test.key1." + new Date().getTime();
        String value1 = "Test Key #1";

        final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
        final KeyValueCache cache = CacheLocator.getKeyValueCache();
        final Contentlet contentlet = LocalTransaction.wrapReturnWithListeners( ()->createTestKeyValueContent(key1, value1, englishLanguageId,
                keyValueContentType, systemUser));

        Assert.assertTrue("Failed creating a new Contentlet using the Key/Value Content Type.",
                        UtilMethods.isSet(contentlet.getIdentifier()));

        KeyValue testKeyValue = keyValueAPI.fromContentlet(contentlet);
        KeyValue keyValue =
                        keyValueAPI.get(testKeyValue.getKey(), englishLanguageId, keyValueContentType, systemUser, Boolean.FALSE);
        KeyValue cachedKeyValue = cache
                .get(key1, englishLanguageId, keyValueContentType.id(), true);

        Assert.assertNotNull("Key/Value cache MUST NOT be null.", cachedKeyValue);

        final String newValue = keyValue.getValue() + ".updatedvalue";
        final Contentlet newContentlet = LocalTransaction.wrapReturnWithListeners(
                ()->updateTestKeyValueContent(contentlet, keyValue.getKey(), newValue, englishLanguageId, keyValueContentType,systemUser));
        cachedKeyValue = cache
                .get(key1, englishLanguageId, keyValueContentType.id(), true);

        System.out.print("cachedKeyValue: " + cachedKeyValue + "\n");
        Assert.assertNull("Key/Value cache MUST BE null.", cachedKeyValue);

        deleteContentlets(systemUser, newContentlet);
    }

    /*
     * Returning a list of KeyValues which have the same key.
     */
    @Test
    public void getKeyValueList() throws DotContentletStateException, IllegalArgumentException,
                    DotDataException, DotSecurityException {

        String key1 = "com.dotcms.test.key1." + new Date().getTime();
        String value1 = "Test Key #1";

        final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
        final Contentlet contentlet =
                createTestKeyValueContent(key1, value1, englishLanguageId, keyValueContentType,
                        systemUser);
        final Contentlet contentlet2 =
                createTestKeyValueContent(contentlet.getIdentifier(), key1, value1 + "spanish",
                        spanishLanguageId,
                        keyValueContentType, systemUser);
        final List<KeyValue> keyValueList = keyValueAPI
                .get(key1, systemUser, keyValueContentType, Boolean.FALSE);

        Assert.assertTrue(UtilMethods.isSet(contentlet.getIdentifier()));
        Assert.assertTrue(UtilMethods.isSet(contentlet2.getIdentifier()));
        Assert.assertTrue("Key/Value list CANNOT BE empty.", !keyValueList.isEmpty());

        deleteContentlets(systemUser, contentlet, contentlet2);
    }

    /*
     * Testing the cache primary group verifying that the cache is empty at the beginning and filled
     * correctly at the end
     */
    @Test
    public void keyValueCache() throws DotContentletStateException, IllegalArgumentException,
                    DotDataException, DotSecurityException {

        String key1 = "com.dotcms.test.key1." + new Date().getTime();
        String value1 = "Test Key #1";

        final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
        final KeyValueCache cache = CacheLocator.getKeyValueCache();
        cache.clearCache();
        final Contentlet contentlet =
                createTestKeyValueContent(key1, value1, englishLanguageId, keyValueContentType,
                        systemUser);
        List<KeyValue> keyValues = cache.get(key1);

        Assert.assertNull(String.format("Key/Value cache MUST BE EMPTY at this point. It had %s elements.",
                        null != keyValues ? keyValues.size() : ""), keyValues);

        KeyValue testKeyValue = keyValueAPI.fromContentlet(contentlet);
        keyValues = keyValueAPI.get(testKeyValue.getKey(), systemUser, keyValueContentType, Boolean.FALSE);

        Assert.assertTrue(String.format("Key/Value list MUST CONTAIN 1 element. It had %s elements.", keyValues.size()),
                        keyValues.size() == 1);
        Assert.assertTrue(String.format("Key/Value cache MUST CONTAIN 1 element. It had %s elements.",
                        cache.get(testKeyValue.getKey()).size()), cache.get(testKeyValue.getKey()).size() == 1);

        deleteContentlets(systemUser, contentlet);
    }

    /*
     * Testing the cache byLanguageGroup verifying that the cache is empty at the beginning and
     * filled correctly at the end
     */
    @Test
    public void keyValueLanguageCache() throws DotContentletStateException,
                    IllegalArgumentException, DotDataException, DotSecurityException {

        String key1 = "com.dotcms.test.key1." + new Date().getTime();
        String value1 = "Test Key #1";

        final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
        final KeyValueCache cache = CacheLocator.getKeyValueCache();
        cache.clearCache();
        final Contentlet contentlet =
                createTestKeyValueContent(key1, value1, englishLanguageId, keyValueContentType,
                        systemUser);
        List<KeyValue> keyValues = cache.getByLanguage(key1, englishLanguageId);

        Assert.assertNull(String.format("Key/Value cache MUST BE EMPTY at this point. It had %s elements.",
                null != cache.get(key1) ? cache.get(key1) : ""), cache.get(key1));

        KeyValue testKeyValue = keyValueAPI.fromContentlet(contentlet);

        System.out.print("testKeyValue: " + testKeyValue + ", keyValueAPI = " + keyValueAPI + "\n");
        System.out.print("testKeyValue.getKey: " + testKeyValue.getKey()
                + ", testKeyValue.getLanguageId = " + testKeyValue.getLanguageId() + "\n");
        keyValues = keyValueAPI.get(testKeyValue.getKey(), keyValueContentType, testKeyValue.getLanguageId(), systemUser, Boolean.FALSE);

        System.out.print("keyValues: " + keyValues  + "\n");
        Assert.assertTrue(String.format("Key/Value list MUST CONTAIN 1 element. It had %s elements.", keyValues.size()),
                        keyValues.size() == 1);
        Assert.assertTrue(
                        String.format("Key/Value cache MUST CONTAIN 1 element. It had %s elements.",
                                        cache.getByLanguage(testKeyValue.getKey(), englishLanguageId).size()),
                        cache.getByLanguage(testKeyValue.getKey(), englishLanguageId).size() == 1);

        deleteContentlets(systemUser, contentlet);
    }

    /*
     * Testing the cache byContentTypeGroup verifying that the cache is empty at the beginning and
     * filled correctly at the end
     */
    @Test
    public void keyValueContentTypeCache() throws DotContentletStateException,
                    IllegalArgumentException, DotDataException, DotSecurityException {

        String key1 = "com.dotcms.test.key1." + new Date().getTime();
        String value1 = "Test Key #1";

        final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
        final KeyValueCache cache = CacheLocator.getKeyValueCache();
        cache.clearCache();
        final Contentlet contentlet =
                createTestKeyValueContent(key1, value1, englishLanguageId, keyValueContentType,
                        systemUser);

        List<KeyValue> keyValues = cache.getByContentType(key1, keyValueContentType.id());

        Assert.assertNull(String.format("Key/Value cache MUST BE EMPTY at this point. It had %s elements.",
                null != cache.get(key1) ? cache.get(key1) : ""), cache.get(key1));

        KeyValue testKeyValue = keyValueAPI.fromContentlet(contentlet);
        keyValues = keyValueAPI.get(testKeyValue.getKey(), keyValueContentType, systemUser, Boolean.FALSE);

        Assert.assertTrue(String.format("Key/Value list MUST CONTAIN 1 element. It had %s elements.", keyValues.size()),
                        keyValues.size() == 1);
        Assert.assertTrue(
                        String.format("Key/Value cache MUST CONTAIN 1 element. It had %s elements.",
                                        cache.getByContentType(testKeyValue.getKey(), keyValueContentType.id()).size()),
                        cache.getByContentType(testKeyValue.getKey(), keyValueContentType.id()).size() == 1);

        deleteContentlets(systemUser, contentlet);
    }

    /*
     * Testing the cache byLanguageContentTypeGroup verifying that the cache is empty at the
     * beginning and filled correctly at the end
     */
    @Test
    public void keyValueLanguageContentTypeCache() throws DotContentletStateException,
                    IllegalArgumentException, DotDataException, DotSecurityException {

        String key1 = "com.dotcms.test.key1." + new Date().getTime();
        String value1 = "Test Key #1";

        final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
        final KeyValueCache cache = CacheLocator.getKeyValueCache();
        cache.clearCache();
        final Contentlet contentlet =
                createTestKeyValueContent(key1, value1, englishLanguageId, keyValueContentType,
                        systemUser);
        KeyValue cachedKeyValue = cache
                .getByLanguageAndContentType(key1, englishLanguageId, keyValueContentType.id());

        Assert.assertNull("Key/Value cache MUST BE NULL at this point.", cachedKeyValue);

        KeyValue testKeyValue = keyValueAPI.fromContentlet(contentlet);
        KeyValue keyValue =
                        keyValueAPI.get(testKeyValue.getKey(), englishLanguageId, keyValueContentType, systemUser, Boolean.FALSE);

        Assert.assertNotNull("Key/Value object MUST NOT be null.", keyValue);
        Assert.assertNotNull("Key/Value cache MUST NOT be null.",
                cache.get(key1, englishLanguageId,
                        keyValueContentType.id(), true));

        deleteContentlets(systemUser, contentlet);
    }

    @AfterClass
    public static void cleanup() throws DotDataException, DotSecurityException {
        if (null != keyValueContentType && UtilMethods.isSet(keyValueContentType.id())) {
            ContentTypeAPI contentTypeApi = APILocator.getContentTypeAPI(systemUser);
            contentTypeApi.delete(keyValueContentType);
        }
        cleanupDebug(KeyValueAPITest.class);
    }

}
