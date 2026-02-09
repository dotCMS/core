package com.dotcms.security.apps;

import static com.dotcms.security.apps.SecretsKeyStoreHelper.SECRETS_KEYSTORE_PASSWORD_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import java.util.Collection;
import java.util.Optional;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class SecretsStoreKeyStoreImplTest {

    private final String LONG_KEY_NAME = new String(
            "A"
                    + "\u00ea"
                    + "\u00f1"
                    + "\u00fc"
                    + "C"
                    + "ASDSADFDDSFasfddsadasdsadsadasdq3r4efwqrrqwerqewrqewrqreqwreqwrqewrqwerqewrqewrqwerqwerqwerqewr43545324542354235243524354325423qwerewds fds fds gf eqgq ewg qeg qe wg egw    ww  eeR ASdsadsadadsadsadsadaqrewq43223t14@#$#@^%$%&%#$sfwf erqwfewfqewfgqewdsfqewtr243fq43f4q444fa4ferfrearge");

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        SecretsStore.INSTANCE.get().deleteAll();
    }

    /**
     * This tests to insure that we have a secrets singleton
     */
    @Test
    public void Test_Secrets_Singleton() throws Exception {

        final SecretsStore secretsStore = SecretsStore.INSTANCE.get();

        final SecretsStore secretsStore1 = SecretsStore.INSTANCE.get();

        assertEquals("we have a SecretsStore singleton", secretsStore, secretsStore1);
    }


    /**
     * This tests storing an retrieving a value from the SecretsStore
     */
    @Test
    public void Test_Storing_a_Value() throws Exception {

        final SecretsStore secretsStore = SecretsStore.INSTANCE.get();

        final String key = UUIDGenerator.generateUuid();
        final String value = UUIDGenerator.generateUuid();

        secretsStore.saveValue(key, value.toCharArray());
        final Optional<char[]> optionalChars = secretsStore.getValue(key);
        assertTrue(optionalChars.isPresent());
        final String returnValue = new String(optionalChars.get());
        assertEquals("stored value is the same", value, returnValue);
    }

    /**
     * This tests storing and retrieving a long value from the SecretsStore
     */
    @Test
    public void Test_Storing_A_Long_Value() {
        final SecretsStore secretsStore = SecretsStore.INSTANCE.get();

        final String key = UUIDGenerator.generateUuid();
        final String value = RandomStringUtils.randomAlphanumeric(2048);

        secretsStore.saveValue(key, value.toCharArray());
        final Optional<char[]> optionalChars = secretsStore.getValue(key);
        assertTrue(optionalChars.isPresent());
        final String returnValue = new String(optionalChars.get());
        assertEquals("stored value is the same", value, returnValue);
    }


    /**
     * This tests storing an retrieving a long keyname in the SecretsStore
     */
    @Test
    public void Test_Storing_A_Long_Key_Name() {
        final SecretsStore secretsStore = SecretsStore.INSTANCE.get();
        final String key = LONG_KEY_NAME;
        final String value = UUIDGenerator.generateUuid();
        secretsStore.saveValue(key, value.toCharArray());
        final Optional<char[]> optionalChars = secretsStore.getValue(key);
        assertTrue(optionalChars.isPresent());
        final String returnValue = new String(optionalChars.get());
        assertEquals("stored value is the same", value, returnValue);
    }

    /**
     * Does storing values under the same key overrides the old value
     */
    @Test
    public void Test_Storing_A_Value_Key_Name_Override() {
        final SecretsStore secretsStore = SecretsStore.INSTANCE.get();
        final String key = "Any";
        final String value1 = "value-1";
        final String value2 = "value-2";
        secretsStore.saveValue(key, value1.toCharArray());
        final Optional<char[]> optionalChars1 = secretsStore.getValue(key);
        assertTrue(optionalChars1.isPresent());
        final String returnValue1 = new String(optionalChars1.get());
        assertEquals("stored value is the same", value1, returnValue1);

        secretsStore.saveValue(key, value2.toCharArray());
        final Optional<char[]> optionalChars2 = secretsStore.getValue(key);
        assertTrue(optionalChars2.isPresent());
        final String returnValue2 = new String(optionalChars2.get());
        assertEquals("stored value is now the second value", value2, returnValue2);
    }

    @Test
    public void Test_404_Cache() {
        final SecretsStore secretsStore = SecretsStore.INSTANCE.get();

        final String key = UUIDGenerator.generateUuid();
        final String value = UUIDGenerator.generateUuid();

        Optional<char[]> noValue = secretsStore.getValue(key);

        assertFalse(noValue.isPresent());

        noValue = secretsStore.getValue(key);

        assertFalse(noValue.isPresent());

        final char[] CACHE_404 = (char[]) CacheLocator.getCacheAdministrator()
                .getNoThrow(key, AppsCacheImpl.SECRETS_CACHE_GROUP);
        assertNotNull(CACHE_404);
        assertEquals(CACHE_404, AppsCacheImpl.CACHE_404);

        secretsStore.saveValue(key, value.toCharArray());
        final Optional<char[]> optionalChars = secretsStore.getValue(key);
        assertTrue(optionalChars.isPresent());
        final String returnValue = new String(optionalChars.get());

        assertEquals(value, returnValue);
    }


    @Test
    public void Test_Deleting_A_Value() {

        final SecretsStore secretsStore = SecretsStore.INSTANCE.get();

        final String key = UUIDGenerator.generateUuid();
        final String value = UUIDGenerator.generateUuid();
        secretsStore.saveValue(key, value.toCharArray());

        assertTrue (secretsStore.getValue(key).isPresent());
        secretsStore.deleteValue(key);
        assertFalse(secretsStore.getValue(key).isPresent());

    }


    @Test
    public void Test_Empty_Value() {
        final String uuid = UUIDGenerator.generateUuid();
        final SecretsStore secretsStore = SecretsStore.INSTANCE.get();
        final Optional<char[]> val = secretsStore.getValue(uuid);
        assertFalse(val.isPresent());
    }

    /**
     * Given scenario: Three secrets are saved to the store with keys of varying lengths (UUID, UUID, and long key).
     * Expected Result: The listKeys() method should return all three keys in the list, including the long key.
     * This verifies that the cache-aside pattern correctly loads all keys from KeyStore.
     */
    @Test
    public void Test_Value_List() {

        final SecretsStore secretsStore = SecretsStore.INSTANCE.get();

        final String key = UUIDGenerator.generateUuid();
        final String value = UUIDGenerator.generateUuid();

        secretsStore.saveValue(key, value.toCharArray());

        final String key2 = UUIDGenerator.generateUuid();
        final String value2 = UUIDGenerator.generateUuid();

        secretsStore.saveValue(key2, value2.toCharArray());

        final String key3 = RandomStringUtils.randomAlphanumeric(1024).toLowerCase();
        final String value3 = RandomStringUtils.randomAlphanumeric(1024);

        secretsStore.saveValue(key3, value3.toCharArray());

        final Collection<String> keys = secretsStore.listKeys();

        assertTrue (keys.size() > 2);
        assertTrue (keys.contains(key));
        assertTrue (keys.contains(key2));
        assertTrue (keys.contains(key3));
    }


    @Test
    public void Test_Encryption() {
        final String uuid = UUIDGenerator.generateUuid();
        SecretsKeyStoreHelper secretsStore = new SecretsKeyStoreHelper();
        final char[] encrypted = secretsStore.encrypt(uuid.toCharArray());
        assertEquals(uuid,new String((secretsStore).decrypt(encrypted)));

    }

    /**
     * Given scenario: We ensure there's a storage file out there created on top of a password. Then
     * we set a new password to simulate a conflict loading the existing file
     * Expected Result: After changing password we should still be able to interact with the store
     * without getting the UnrecoverableKeyException.
     * But as the previous file just got wiped-out no secret previously stored will be available now.
     */
    @Test
    public void Test_Recovery_On_Load_Failure() {
        final String password = Config.getStringProperty(SECRETS_KEYSTORE_PASSWORD_KEY);
        try {
            final SecretCachedKeyStoreImpl secretsStore = (SecretCachedKeyStoreImpl) SecretsStore.INSTANCE.get();
            final String anyKey = "anyKey-" + System.currentTimeMillis();
            final String anyValue = "anyValue";
            // Save something to ensure there's a file in use.
            secretsStore.saveValue(anyKey, anyValue.toCharArray());
            //Now we change the password.
            Config.setProperty(SECRETS_KEYSTORE_PASSWORD_KEY,
                    RandomStringUtils.randomAlphanumeric(10));
            secretsStore.flushCache();
            //it's a brand new store so do not expect the old key to be there.
            final Optional<char[]> valueInStore = secretsStore.getValue(anyKey);
            assertFalse(valueInStore.isPresent());
        } finally {
            Config.setProperty(SECRETS_KEYSTORE_PASSWORD_KEY, password);
        }
    }

    /**
     * Given scenario: A secret is saved to the store, then the cache is flushed to simulate
     * cache invalidation (e.g., after cluster-wide cache clear or cache expiration).
     * Expected Result: The containsKey() method should still return true after cache flush by
     * using the cache-aside pattern to reload keys from KeyStore. The getValue() method should
     * also return the correct value. Subsequent containsKey() calls should use the repopulated
     * cache. After deletion, containsKey() should return false.
     */
    @Test
    public void Test_ContainsKey_After_Cache_Flush() {
        final SecretsStore secretsStore = SecretsStore.INSTANCE.get();

        final String key = "test-key-" + System.currentTimeMillis();
        final String value = "test-value-" + UUIDGenerator.generateUuid();

        // Step 1: Save a secret (updates both KeyStore and cache)
        secretsStore.saveValue(key, value.toCharArray());

        // Verify it was saved correctly
        assertTrue("Key should exist after saving", secretsStore.containsKey(key));
        assertTrue("Value should be retrievable", secretsStore.getValue(key).isPresent());

        // Step 2: Flush the cache to simulate cache invalidation
        // This is what happens in production when cache is cleared
        final SecretCachedKeyStoreImpl cachedStore = (SecretCachedKeyStoreImpl) secretsStore;
        cachedStore.flushCache();

        // Step 3: Verify containsKey() still works after cache flush
        // This is the critical fix - it should check KeyStore when cache is empty
        assertTrue("Key should still exist after cache flush (fallback to KeyStore)",
                   secretsStore.containsKey(key));

        // Step 4: Verify the value is still retrievable
        final Optional<char[]> retrievedValue = secretsStore.getValue(key);
        assertTrue("Value should still be retrievable after cache flush", retrievedValue.isPresent());
        assertEquals("Retrieved value should match original", value, new String(retrievedValue.get()));

        // Step 5: Verify subsequent calls still work (cache has been repopulated)
        assertTrue("Key should exist on subsequent check", secretsStore.containsKey(key));

        // Cleanup
        secretsStore.deleteValue(key);
        assertFalse("Key should not exist after deletion", secretsStore.containsKey(key));
    }

    /**
     * Given scenario: Multiple secrets are saved to the store, then the cache is flushed to
     * simulate cache invalidation in a multi-node cluster environment.
     * Expected Result: All keys should remain accessible through containsKey() after cache flush.
     * The listKeys() method should also return all keys correctly. This verifies that the
     * cache-aside pattern properly reloads all keys from KeyStore and does not lose any entries
     * during cache repopulation.
     */
    @Test
    public void Test_Multiple_ContainsKey_After_Cache_Flush() {
        final SecretsStore secretsStore = SecretsStore.INSTANCE.get();
        final SecretCachedKeyStoreImpl cachedStore = (SecretCachedKeyStoreImpl) secretsStore;

        final String key1 = "test-multi-key1-" + System.currentTimeMillis();
        final String key2 = "test-multi-key2-" + System.currentTimeMillis();
        final String key3 = "test-multi-key3-" + System.currentTimeMillis();
        final String value = "test-value";

        // Save multiple secrets
        secretsStore.saveValue(key1, value.toCharArray());
        secretsStore.saveValue(key2, value.toCharArray());
        secretsStore.saveValue(key3, value.toCharArray());

        // Verify all keys exist
        assertTrue("Key1 should exist", secretsStore.containsKey(key1));
        assertTrue("Key2 should exist", secretsStore.containsKey(key2));
        assertTrue("Key3 should exist", secretsStore.containsKey(key3));

        // Flush cache
        cachedStore.flushCache();

        // Verify all keys still exist after cache flush
        assertTrue("Key1 should exist after cache flush", secretsStore.containsKey(key1));
        assertTrue("Key2 should exist after cache flush", secretsStore.containsKey(key2));
        assertTrue("Key3 should exist after cache flush", secretsStore.containsKey(key3));

        // Verify listKeys() also returns all keys
        final Collection<String> keys = secretsStore.listKeys();
        assertTrue("Keys list should contain key1", keys.contains(key1));
        assertTrue("Keys list should contain key2", keys.contains(key2));
        assertTrue("Keys list should contain key3", keys.contains(key3));

        // Cleanup
        secretsStore.deleteValue(key1);
        secretsStore.deleteValue(key2);
        secretsStore.deleteValue(key3);
    }


}
