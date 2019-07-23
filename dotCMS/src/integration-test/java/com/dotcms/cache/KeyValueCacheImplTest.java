package com.dotcms.cache;

import com.dotcms.keyvalue.model.DefaultKeyValue;
import com.dotcms.keyvalue.model.KeyValue;
import com.dotcms.util.IntegrationTestInitService;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class KeyValueCacheImplTest {

    @BeforeClass
    public static void prepare() throws Exception{
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void shouldAddCacheEntryForContentTypeAndLanguage() {
        final String key = "key";
        final String value = "value";
        final long languageId = 2;
        final String contentTypeId = "1";

        final KeyValue keyValue = new DefaultKeyValue();
        keyValue.setKey(key);
        keyValue.setValue(value);

        KeyValueCache keyValueCache = new KeyValueCacheImpl();
        keyValueCache.addByLanguageAndContentType(languageId, contentTypeId, keyValue);

        final KeyValue keyValueReturned = keyValueCache.getByLanguageAndContentType(key, languageId, contentTypeId);
        assertEquals(keyValue, keyValueReturned);
    }

    @Test
    public void shouldReturnNullWhenLookForKeyByContentTypeAndLanguageIdNotAdded() {
        final String key = "key";
        final long languageId = 2;
        final String contentTypeId = "1";

        KeyValueCache keyValueCache = new KeyValueCacheImpl();

        final KeyValue keyValueReturned = keyValueCache.getByLanguageAndContentType(key, languageId, contentTypeId);

        assertNull(keyValueReturned);
    }

    @Test
    public void shouldReturnNullWhenLookForKeyByContentTypeAndWrongLanguageId() {
        final String key = "key";
        final String value = "value";
        final long languageId = 2;
        final String contentTypeId = "1";

        final KeyValue keyValue = new DefaultKeyValue();
        keyValue.setKey(key);
        keyValue.setValue(value);

        KeyValueCache keyValueCache = new KeyValueCacheImpl();
        keyValueCache.addByLanguageAndContentType(languageId, contentTypeId, keyValue);

        final KeyValue keyValueReturned = keyValueCache.getByLanguageAndContentType(key, 1, contentTypeId);
        assertNull(keyValueReturned);
    }

    @Test
    public void shouldReturnNullWhenLookForKeyByLanguageidAndWrongContentTypeAnd() {
        final String key = "key";
        final String value = "value";
        final long languageId = 2;
        final String contentTypeId = "1";

        final KeyValue keyValue = new DefaultKeyValue();
        keyValue.setKey(key);
        keyValue.setValue(value);

        KeyValueCache keyValueCache = new KeyValueCacheImpl();
        keyValueCache.addByLanguageAndContentType(languageId, contentTypeId, keyValue);

        final KeyValue keyValueReturned = keyValueCache.getByLanguageAndContentType(key, languageId, "2");
        assertNull(keyValueReturned);
    }

    @Test
    public void shouldAddCacheEntryForContentTypeLanguageAndNotLive() {
        final String key = "key";
        final String value = "value";
        final long languageId = 2;
        final String contentTypeId = "1";
        final boolean live = false;

        final KeyValue keyValue = new DefaultKeyValue();
        keyValue.setKey(key);
        keyValue.setValue(value);

        KeyValueCache keyValueCache = new KeyValueCacheImpl();
        keyValueCache.add(languageId, contentTypeId, live, keyValue);

        final KeyValue keyValueReturned = keyValueCache.get(key, languageId, contentTypeId, live);

        assertEquals(keyValue, keyValueReturned);
    }


    @Test
    public void shouldReturnNullWhenLookForKeyByContentTypeLanguageLiveIdNotAdded() {
        final String key = "key";
        final long languageId = 2;
        final String contentTypeId = "1";
        final boolean live = true;

        KeyValueCache keyValueCache = new KeyValueCacheImpl();

        final KeyValue keyValueReturned = keyValueCache.get(key, languageId, contentTypeId, live);

        assertNull(keyValueReturned);
    }

    @Test
    public void shouldReturnNullWhenLookForKeyByContentTypeLanguageAndWrongLive() {
        final String key = "key";
        final String value = "value";
        final long languageId = 2;
        final String contentTypeId = "1";
        final boolean live = true;

        final KeyValue keyValue = new DefaultKeyValue();
        keyValue.setKey(key);
        keyValue.setValue(value);

        KeyValueCache keyValueCache = new KeyValueCacheImpl();
        keyValueCache.add(languageId, contentTypeId, live, keyValue);

        final KeyValue keyValueReturned = keyValueCache.get(key, languageId, contentTypeId, false);

        assertNull(keyValueReturned);
    }

    @Test
    public void shouldReturnNullWhenLookForKeyByContentTypeLiveAndWrongLanguage() {
        final String key = "key";
        final String value = "value";
        final long languageId = 2;
        final String contentTypeId = "1";
        final boolean live = true;

        final KeyValue keyValue = new DefaultKeyValue();
        keyValue.setKey(key);
        keyValue.setValue(value);

        KeyValueCache keyValueCache = new KeyValueCacheImpl();
        keyValueCache.add(languageId, contentTypeId, live, keyValue);

        final KeyValue keyValueReturned = keyValueCache.get(key, 1, contentTypeId, live);

        assertNull(keyValueReturned);
    }

    @Test
    public void shouldReturnNullWhenLookForKeyByLanguageLiveAndWrongContentType() {
        final String key = "key";
        final String value = "value";
        final long languageId = 2;
        final String contentTypeId = "1";
        final boolean live = true;

        final KeyValue keyValue = new DefaultKeyValue();
        keyValue.setKey(key);
        keyValue.setValue(value);

        KeyValueCache keyValueCache = new KeyValueCacheImpl();
        keyValueCache.add(languageId, contentTypeId, live, keyValue);

        final KeyValue keyValueReturned = keyValueCache.get(key, languageId, "2", live);

        assertNull(keyValueReturned);
    }

}
