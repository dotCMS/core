package com.dotcms.api.content;

import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.IntegrationTestBase;
import com.dotcms.cache.KeyValueCache;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.keyvalue.business.KeyValueAPI;
import com.dotcms.keyvalue.model.KeyValue;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

/**
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

    private static final String KEY_1 = "com.dotcms.test.key1";
    private static final String VALUE_1 = "Test Key #1";

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        // Test Key/Value Content Type
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
        spanishLanguageId = APILocator.getLanguageAPI().getLanguage("es", "ES").getId();
    }

    @Test
    public void saveKeyValueContent() throws DotContentletValidationException, DotContentletStateException,
                    IllegalArgumentException, DotDataException, DotSecurityException {
        final Contentlet contentlet = createTestKeyValueContent(KEY_1, VALUE_1, englishLanguageId);

        Assert.assertTrue("Failed creating a new Contentlet using the Key/Value Content Type.",
                        UtilMethods.isSet(contentlet.getIdentifier()));

        deleteContentlets(contentlet);
    }

    @Test
    public void updateKeyValueContent() throws DotContentletValidationException, DotContentletStateException,
                    IllegalArgumentException, DotDataException, DotSecurityException {
        final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
        final KeyValueCache cache = CacheLocator.getKeyValueCache();
        Contentlet contentlet = createTestKeyValueContent(KEY_1, VALUE_1, englishLanguageId);

        Assert.assertTrue("Failed creating a new Contentlet using the Key/Value Content Type.",
                        UtilMethods.isSet(contentlet.getIdentifier()));

        KeyValue testKeyValue = keyValueAPI.fromContentlet(contentlet);
        KeyValue keyValue =
                        keyValueAPI.get(testKeyValue.getKey(), englishLanguageId, keyValueContentType, systemUser, Boolean.FALSE);
        KeyValue cachedKeyValue = cache.getByLanguageAndContentType(KEY_1, englishLanguageId, keyValueContentType.id());

        Assert.assertNotNull("Key/Value cache MUST NOT be null.", cachedKeyValue);

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final String newValue = keyValue.getValue() + ".updatedvalue";
        final Map<String, Field> fields = keyValueContentType.fieldMap();
        contentletAPI.setContentletProperty(contentlet, asOldField(fields.get(KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR)),
                        newValue);
        // IMPORTANT: Updating a contentlet requires the Inode to be empty. Otherwise, an exception will be thrown
        contentlet.setInode(StringPool.BLANK);
        contentlet = contentletAPI.checkin(contentlet, systemUser, Boolean.FALSE);
        cachedKeyValue = cache.getByLanguageAndContentType(KEY_1, englishLanguageId, keyValueContentType.id());

        Assert.assertNull("Key/Value cache MUST be null.", cachedKeyValue);

        deleteContentlets(contentlet);
    }

    @Test
    public void getKeyValueList() throws DotContentletValidationException, DotContentletStateException, IllegalArgumentException,
                    DotDataException, DotSecurityException {
        final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
        final Contentlet contentlet = createTestKeyValueContent(KEY_1, VALUE_1, englishLanguageId);
        final Contentlet contentlet2 = createTestKeyValueContent(KEY_1, VALUE_1 + "spanish", spanishLanguageId);
        final List<KeyValue> keyValueList = keyValueAPI.get(KEY_1, systemUser, Boolean.FALSE);

        Assert.assertTrue("Failed creating a new Contentlet using the Key/Value Content Type.",
                        UtilMethods.isSet(contentlet.getIdentifier()));
        Assert.assertTrue("Failed creating a new Contentlet using the Key/Value Content Type.",
                        UtilMethods.isSet(contentlet2.getIdentifier()));
        Assert.assertTrue("Key/Value list cannot be empty.", !keyValueList.isEmpty());

        deleteContentlets(contentlet, contentlet2);
    }

    @Test
    public void keyValueCache() throws DotContentletValidationException, DotContentletStateException, IllegalArgumentException,
                    DotDataException, DotSecurityException {
        final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
        final KeyValueCache cache = CacheLocator.getKeyValueCache();
        cache.clearCache();
        final Contentlet contentlet = createTestKeyValueContent(KEY_1, VALUE_1, englishLanguageId);
        List<KeyValue> keyValues = cache.get(KEY_1);

        Assert.assertNull(String.format("Key/Value cache MUST BE EMPTY at this point. It had %s elements.",
                        null != keyValues ? keyValues.size() : ""), keyValues);

        KeyValue testKeyValue = keyValueAPI.fromContentlet(contentlet);
        keyValues = keyValueAPI.get(testKeyValue.getKey(), systemUser, Boolean.FALSE);

        Assert.assertTrue(String.format("Key/Value list MUST CONTAIN 1 element. It had %s elements.", keyValues.size()),
                        keyValues.size() == 1);
        Assert.assertTrue(String.format("Key/Value cache MUST CONTAIN 1 element. It had %s elements.",
                        cache.get(testKeyValue.getKey()).size()), cache.get(testKeyValue.getKey()).size() == 1);

        deleteContentlets(contentlet);
    }

    @Test
    public void keyValueLanguageCache() throws DotContentletValidationException, DotContentletStateException,
                    IllegalArgumentException, DotDataException, DotSecurityException {
        final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
        final KeyValueCache cache = CacheLocator.getKeyValueCache();
        cache.clearCache();
        final Contentlet contentlet = createTestKeyValueContent(KEY_1, VALUE_1, englishLanguageId);
        List<KeyValue> keyValues = cache.getByLanguage(KEY_1, englishLanguageId);

        Assert.assertNull(String.format("Key/Value cache MUST BE EMPTY at this point. It had %s elements.",
                        null != cache.get(KEY_1) ? cache.get(KEY_1) : ""), cache.get(KEY_1));

        KeyValue testKeyValue = keyValueAPI.fromContentlet(contentlet);
        keyValues = keyValueAPI.get(testKeyValue.getKey(), testKeyValue.getLanguageId(), systemUser, Boolean.FALSE);

        Assert.assertTrue(String.format("Key/Value list MUST CONTAIN 1 element. It had %s elements.", keyValues.size()),
                        keyValues.size() == 1);
        Assert.assertTrue(
                        String.format("Key/Value cache MUST CONTAIN 1 element. It had %s elements.",
                                        cache.getByLanguage(testKeyValue.getKey(), englishLanguageId).size()),
                        cache.getByLanguage(testKeyValue.getKey(), englishLanguageId).size() == 1);

        deleteContentlets(contentlet);
    }

    @Test
    public void keyValueContentTypeCache() throws DotContentletValidationException, DotContentletStateException,
                    IllegalArgumentException, DotDataException, DotSecurityException {
        final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
        final KeyValueCache cache = CacheLocator.getKeyValueCache();
        cache.clearCache();
        final Contentlet contentlet = createTestKeyValueContent(KEY_1, VALUE_1, englishLanguageId);
        List<KeyValue> keyValues = cache.getByContentType(KEY_1, keyValueContentType.id());

        Assert.assertNull(String.format("Key/Value cache MUST BE EMPTY at this point. It had %s elements.",
                        null != cache.get(KEY_1) ? cache.get(KEY_1) : ""), cache.get(KEY_1));

        KeyValue testKeyValue = keyValueAPI.fromContentlet(contentlet);
        keyValues = keyValueAPI.get(testKeyValue.getKey(), keyValueContentType, systemUser, Boolean.FALSE);

        Assert.assertTrue(String.format("Key/Value list MUST CONTAIN 1 element. It had %s elements.", keyValues.size()),
                        keyValues.size() == 1);
        Assert.assertTrue(
                        String.format("Key/Value cache MUST CONTAIN 1 element. It had %s elements.",
                                        cache.getByContentType(testKeyValue.getKey(), keyValueContentType.id()).size()),
                        cache.getByContentType(testKeyValue.getKey(), keyValueContentType.id()).size() == 1);

        deleteContentlets(contentlet);
    }

    @Test
    public void keyValueLanguageContentTypeCache() throws DotContentletValidationException, DotContentletStateException,
                    IllegalArgumentException, DotDataException, DotSecurityException {
        final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
        final KeyValueCache cache = CacheLocator.getKeyValueCache();
        cache.clearCache();
        final Contentlet contentlet = createTestKeyValueContent(KEY_1, VALUE_1, englishLanguageId);
        KeyValue cachedKeyValue = cache.getByLanguageAndContentType(KEY_1, englishLanguageId, keyValueContentType.id());

        Assert.assertNull("Key/Value cache MUST BE NULL at this point.", cachedKeyValue);

        KeyValue testKeyValue = keyValueAPI.fromContentlet(contentlet);
        KeyValue keyValue =
                        keyValueAPI.get(testKeyValue.getKey(), englishLanguageId, keyValueContentType, systemUser, Boolean.FALSE);

        Assert.assertNotNull("Key/Value object MUST NOT be null.", keyValue);
        Assert.assertNotNull("Key/Value cache MUST NOT be null.",
                        cache.getByLanguageAndContentType(KEY_1, englishLanguageId, keyValueContentType.id()));

        deleteContentlets(contentlet);
    }

    /*
     * @Test public void tempDelete() throws DotContentletValidationException,
     * DotContentletStateException, IllegalArgumentException, DotDataException, DotSecurityException
     * { final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI(); final KeyValueCache cache =
     * CacheLocator.getKeyValueCache(); cache.clearCache(); List<KeyValue> keyValues =
     * cache.get(KEY_1);
     * 
     * keyValues = keyValueAPI.get(KEY_1, systemUser, Boolean.FALSE);
     * 
     * deleteContentlets(Contentlet.class.cast(keyValues.get(0))); }
     */

    @AfterClass
    public static void cleanup() throws DotDataException, DotSecurityException {
        if (null != keyValueContentType && UtilMethods.isSet(keyValueContentType.id())) {
            ContentTypeAPI contentTypeApi = APILocator.getContentTypeAPI(systemUser);
            contentTypeApi.delete(keyValueContentType);
        }
    }

    /**
     * 
     * @param key
     * @param value
     * @param languageId
     * @return
     * @throws DotContentletValidationException
     * @throws DotContentletStateException
     * @throws IllegalArgumentException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private Contentlet createTestKeyValueContent(final String key, final String value, final long languageId)
                    throws DotContentletValidationException, DotContentletStateException, IllegalArgumentException,
                    DotDataException, DotSecurityException {
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        Contentlet contentlet = new Contentlet();
        contentlet.setContentTypeId(keyValueContentType.inode());
        contentlet.setLanguageId(englishLanguageId);
        final Map<String, Field> fields = keyValueContentType.fieldMap();
        contentletAPI.setContentletProperty(contentlet, asOldField(fields.get(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR)), key);
        contentletAPI.setContentletProperty(contentlet, asOldField(fields.get(KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR)),
                        value);
        contentlet = contentletAPI.checkin(contentlet, systemUser, Boolean.FALSE);
        contentletAPI.publish(contentlet, systemUser, Boolean.FALSE);
        contentletAPI.isInodeIndexed(contentlet.getInode());
        return contentlet;
    }

    /**
     * Utility method used to transform a new {@link Field} object into a legacy
     * {@link com.dotmarketing.portlets.structure.model.Field}.
     * 
     * @param newField - The new {@link Field} object that will be transformed.
     * @return The legacy {@link com.dotmarketing.portlets.structure.model.Field} representation.
     */
    private com.dotmarketing.portlets.structure.model.Field asOldField(final Field newField) {
        return new LegacyFieldTransformer(newField).asOldField();
    }

    /**
     * 
     * @param contentlets
     * @throws DotContentletStateException
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    private void deleteContentlets(final Contentlet... contentlets)
                    throws DotContentletStateException, DotDataException, DotSecurityException {
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        for (Contentlet contentlet : contentlets) {
            contentletAPI.unpublish(contentlet, systemUser, Boolean.FALSE);
            contentletAPI.archive(contentlet, systemUser, Boolean.FALSE);
            contentletAPI.delete(contentlet, systemUser, Boolean.FALSE);
        }
    }

}
