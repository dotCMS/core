package com.dotcms.cache;

import com.dotcms.keyvalue.model.DefaultKeyValue;
import com.dotcms.keyvalue.model.KeyValue;
import com.dotcms.util.IntegrationTestInitService;
import java.util.Random;
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
        final long random = new Random().nextLong();
        final String key = "key" + random;
        final long languageId = random;
        final String contentTypeId = String.valueOf(random);

        KeyValueCache keyValueCache = new KeyValueCacheImpl();

        final KeyValue keyValueReturned = keyValueCache.getByLanguageAndContentType(key, languageId, contentTypeId);

        assertNull(keyValueReturned);
    }

    @Test
    public void shouldReturnNullWhenLookForKeyByContentTypeAndWrongLanguageId() {
        final long time = System.currentTimeMillis();
        final String key = "key" + time;
        final String value = "value";
        final long languageId = time;
        final String contentTypeId = String.valueOf(time);
        final long wrongLangId = 50;

        final KeyValue keyValue = new DefaultKeyValue();
        keyValue.setKey(key);
        keyValue.setValue(value);

        KeyValueCache keyValueCache = new KeyValueCacheImpl();
        keyValueCache.addByLanguageAndContentType(languageId, contentTypeId, keyValue);

        final KeyValue keyValueReturned = keyValueCache.getByLanguageAndContentType(key, wrongLangId, contentTypeId);
        assertNull(keyValueReturned);
    }

    @Test
    public void shouldReturnNullWhenLookForKeyByLanguageidAndWrongContentTypeAnd() {
        final long time = System.currentTimeMillis();
        final String key = "key" + time;
        final String value = "value";
        final long languageId = time;
        final String contentTypeId = String.valueOf(time);
        final String wrongContentTypeId =  "xyz";

        final KeyValue keyValue = new DefaultKeyValue();
        keyValue.setKey(key);
        keyValue.setValue(value);

        KeyValueCache keyValueCache = new KeyValueCacheImpl();
        keyValueCache.addByLanguageAndContentType(languageId, contentTypeId, keyValue);

        final KeyValue keyValueReturned = keyValueCache.getByLanguageAndContentType(key, languageId, wrongContentTypeId);
        assertNull(keyValueReturned);
    }

    @Test
    public void shouldAddCacheEntryForContentTypeLanguageAndNotLive() {

        final long time = System.currentTimeMillis();
        final String key = "key" + time;
        final String value = "value";
        final long languageId = time;
        final String contentTypeId = String.valueOf(time);
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
        final long random = new Random().nextLong();
        final String key = "key" + random;
        final long languageId = random;
        final String contentTypeId = String.valueOf(random);
        final boolean live = true;

        KeyValueCache keyValueCache = new KeyValueCacheImpl();

        final KeyValue keyValueReturned = keyValueCache.get(key, languageId, contentTypeId, live);

        assertNull(keyValueReturned);
    }

    @Test
    public void shouldReturnNullWhenLookForKeyByContentTypeLanguageAndWrongLive() {
        final long time = System.currentTimeMillis();
        final String key = "key" + time;
        final String value = "value";
        final long languageId = time;
        final String contentTypeId = String.valueOf(time);
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
        final long time = System.currentTimeMillis();
        final String key = "key" + time;
        final String value = "value";
        final long languageId = time;
        final String contentTypeId = String.valueOf(time);
        final boolean live = true;
        final long wrongLanguageId = 50;

        final KeyValue keyValue = new DefaultKeyValue();
        keyValue.setKey(key);
        keyValue.setValue(value);

        KeyValueCache keyValueCache = new KeyValueCacheImpl();
        keyValueCache.add(languageId, contentTypeId, live, keyValue);

        final KeyValue keyValueReturned = keyValueCache.get(key, wrongLanguageId, contentTypeId, live);

        assertNull(keyValueReturned);
    }

    @Test
    public void shouldReturnNullWhenLookForKeyByLanguageLiveAndWrongContentType() {
        final long time = System.currentTimeMillis();
        final String key = "key" + time;
        final String value = "value";
        final long languageId = time;
        final String contentTypeId = String.valueOf(time);
        final boolean live = true;
        final String wrongContentTypeId = "123123";

        final KeyValue keyValue = new DefaultKeyValue();
        keyValue.setKey(key);
        keyValue.setValue(value);

        final KeyValueCache keyValueCache = new KeyValueCacheImpl();
        keyValueCache.add(languageId, contentTypeId, live, keyValue);

        final KeyValue keyValueReturned = keyValueCache.get(key, languageId, wrongContentTypeId, live);

        assertNull(keyValueReturned);
    }

}
