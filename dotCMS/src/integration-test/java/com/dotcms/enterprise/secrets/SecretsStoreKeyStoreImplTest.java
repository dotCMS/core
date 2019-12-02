package com.dotcms.enterprise.secrets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Collection;
import java.util.Optional;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.UUIDGenerator;

public class SecretsStoreKeyStoreImplTest {
    final String LONG_KEYNAME = new String(
                    "A"
                            + "\u00ea"
                            + "\u00f1"
                            + "\u00fc"
                            + "C"
                            + "ASDSADFDDSFasfddsadasdsadsadasdq3r4efwqrrqwerqewrqewrqreqwreqwrqewrqwerqewrqewrqwerqwerqwerqewr43545324542354235243524354325423qwerewds fds fds gf eqgq ewg qeg qe wg egw    ww  eeR ASdsadsadadsadsadsadaqrewq43223t14@#$#@^%$%&%#$sfwf erqwfewfqewfgqewdsfqewtr243fq43f4q444fa4ferfrearge");
    
    @BeforeClass
    public static void prepare() throws Exception{
        IntegrationTestInitService.getInstance().init();
        SecretsStore.INSTANCE.get().deleteAll();
    }

    /**
     * This tests to insure that we have a secrets
     * singleton
     * @throws Exception
     */
    @Test
    public void test_secrets_singleton() throws Exception{

        SecretsStore ssapi = SecretsStore.INSTANCE.get();
        
        
        SecretsStore ssapi2 = SecretsStore.INSTANCE.get();
        

        assertTrue("we have a SecretsStore singleon", ssapi==ssapi2);
    }


    
    /**
     * This tests storing an retreiving a value from the SecretsStore
     * @throws Exception
     */
    @Test
    public void test_storing_a_value() throws Exception{

        SecretsStore ssapi = SecretsStore.INSTANCE.get();

        String key = UUIDGenerator.generateUuid();
        String value = UUIDGenerator.generateUuid();
        

        ssapi.saveValue(key, value.toCharArray());

        
        String returnValue = new String(ssapi.getValue(key).get());
        
        assertTrue("stored value is the same", value.equals(returnValue));
        
        
    }
    
    /**
     * This tests storing an retreiving a long value from the SecretsStore
     * @throws Exception
     */
    @Test
    public void test_storing_a_long_value() throws Exception{

        SecretsStore ssapi = SecretsStore.INSTANCE.get();

        
        final String key = UUIDGenerator.generateUuid();
        final String value = RandomStringUtils.randomAlphanumeric(2048);

        ssapi.saveValue(key, value.toCharArray());

        
        String returnValue = new String(ssapi.getValue(key).get());
        
        assertTrue("stored value is the same", value.equals(returnValue));
        
        
    }
    
    
    
    /**
     * This tests storing an retreiving a long keyname in the SecretsStore
     * @throws Exception
     */
    @Test
    public void test_storing_a_long_keyname() throws Exception{

        SecretsStore ssapi = SecretsStore.INSTANCE.get();

        
        String key = LONG_KEYNAME;
        String value = UUIDGenerator.generateUuid();
        

        ssapi.saveValue(key, value.toCharArray());

        
        String returnValue = new String(ssapi.getValue(key).get());
        
        assertTrue("stored value is the same", value.equals(returnValue));
        
        
    }
    
    
    @Test
    public void test_404_cache() throws Exception{

        SecretsStore ssapi = SecretsStore.INSTANCE.get();

        final String key = UUIDGenerator.generateUuid();
        final String value = UUIDGenerator.generateUuid();
        
        Optional<char[]> noValue = ssapi.getValue(key);
        
        assertTrue("optional is empty", !noValue.isPresent());
        
        noValue = ssapi.getValue(key);
        
        assertTrue("optional is empty agin", !noValue.isPresent());
        
        char[] CACHE_404 = (char[])CacheLocator.getCacheAdministrator().getNoThrow(key, SecretsStoreKeyStoreImpl.SECRETS_CACHE_GROUP);
        assertTrue(CACHE_404!=null);
        assertEquals(CACHE_404, SecretsStoreKeyStoreImpl.CACHE_404);
        
        
        
        ssapi.saveValue(key, value.toCharArray());
        String returnValue = new String(ssapi.getValue(key).get());

        assertEquals(value, returnValue);
    }
    
    
    
    
    
    @Test
    public void test_deleting_a_value() throws Exception{

        SecretsStore ssapi = SecretsStore.INSTANCE.get();

        
        String key = UUIDGenerator.generateUuid();
        String value = UUIDGenerator.generateUuid();
        ssapi.saveValue(key, value.toCharArray());


        assert(ssapi.getValue(key).isPresent());
        
        ssapi.deleteValue(key);
        
        assert(!ssapi.getValue(key).isPresent());


    }
    
    
    @Test
    public void test_empty_value() throws Exception{

        String uuid = UUIDGenerator.generateUuid();
        SecretsStore ssapi = SecretsStore.INSTANCE.get();

        Optional<char[]> val = ssapi.getValue(uuid);

        assert(!val.isPresent());


    }
    
    /**
     * tests to make sure that the listKeys method
     * returns newly saved keys in their list
     * @throws Exception
     */
    @Test
    public void test_value_list() throws Exception{

        SecretsStore ssapi = SecretsStore.INSTANCE.get();

        String key = UUIDGenerator.generateUuid();
        String value = UUIDGenerator.generateUuid();

        ssapi.saveValue(key, value.toCharArray());


        String key2 = UUIDGenerator.generateUuid();
        String value2 = UUIDGenerator.generateUuid();
        
        ssapi.saveValue(key2, value2.toCharArray());
        
        String key3 = RandomStringUtils.randomAlphanumeric(1024);
        String value3 = RandomStringUtils.randomAlphanumeric(1024);
        
        ssapi.saveValue(key3, value3.toCharArray());
        
        
        Collection<String> keys = ssapi.listKeys();
        assert(ssapi.listKeys().size()>2);
        assert(ssapi.listKeys().contains(key.toLowerCase()));
        assert(ssapi.listKeys().contains(key2.toLowerCase()));
        assert(ssapi.listKeys().contains(key3.toLowerCase()));
    }
    
    
    
    @Test
    public void test_encryption() throws Exception{

        String uuid = UUIDGenerator.generateUuid();
        SecretsStore ssapi = SecretsStore.INSTANCE.get();

        String encrypted =((SecretsStoreKeyStoreImpl)ssapi).encrypt(uuid);

        assert(uuid.equals(new String(((SecretsStoreKeyStoreImpl)ssapi).decrypt(encrypted))));


    }
    
    
    
    
}
