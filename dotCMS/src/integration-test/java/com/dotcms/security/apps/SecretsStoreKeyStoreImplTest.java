package com.dotcms.security.apps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.CacheLocator;
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
                .getNoThrow(key, SecretsStoreKeyStoreImpl.SECRETS_CACHE_GROUP);
        assertNotNull(CACHE_404);
        assertEquals(CACHE_404, SecretsStoreKeyStoreImpl.CACHE_404);

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
     * tests to make sure that the listKeys method returns newly saved keys in their list
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

        final String key3 = RandomStringUtils.randomAlphanumeric(1024);
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
        final SecretsStore secretsStore = SecretsStore.INSTANCE.get();
        final char[] encrypted = ((SecretsStoreKeyStoreImpl) secretsStore).encrypt(uuid.toCharArray());
        assertEquals(uuid,new String(((SecretsStoreKeyStoreImpl) secretsStore).decrypt(encrypted)));

    }


}
