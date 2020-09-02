package com.dotcms.security.apps;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import java.util.Arrays;
import java.util.Map;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class AppsCacheImplTest {

    static AppsCache cache;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        SecretsStore.INSTANCE.get().deleteAll();
        cache = CacheLocator.getAppsCache();
        cache.clearCache();
    }

    /**
     * Simply test a put then a get. Then a get of a non existing key
     */
    @Test
    public void Test_put_Secret_Then_Verify_Get_From_Cache(){
         final String input = RandomStringUtils.randomAlphanumeric(100);
         cache.putSecret("anyKey", input.toCharArray());
         final char [] expectedValue = cache.getFromCache("anyKey");
         Assert.assertEquals(new String(expectedValue), input);
         final char[] nonExisting = cache.getFromCache("non-existing-key");
         Assert.assertNull(nonExisting);
    }

    /**
     * Test a secret is available when set using a supplier.
     * @throws DotCacheException
     */
    @Test
    public void Test_Get_Secret_From_Cache_With_Supplier() throws DotCacheException {
        final String secretKey = "Secret-Key";
        cache.putSecret(secretKey, "oldValue".toCharArray());
        final char [] output = cache.getFromCache(secretKey, "new-Value"::toCharArray);
        Assert.assertEquals(new String(output), "oldValue");
        cache.flushSecret(secretKey);
        final char [] goneValue = cache.getFromCache(secretKey);
        Assert.assertNull(goneValue);
        final char [] replacedValue = cache.getFromCache(secretKey, "New-Replaced-Value"::toCharArray);
        Assert.assertEquals(new String(replacedValue), "New-Replaced-Value");
    }


    /**
     * Test a descriptor is available when set using a supplier.
     * @throws DotCacheException
     */
    @Test
    public void Test_Get_Descriptors_From_Cache_With_Supplier() {

        final Map<String, AppDescriptor> meta = cache.getAppDescriptorsMap(() -> Arrays.asList(
                createDescriptor("k1"),
                createDescriptor("k2")
                )
        );

        Assert.assertEquals(meta.get("k1").getName(),"k1");
        Assert.assertEquals(meta.get("k2").getName(),"k2");
        Assert.assertNull(meta.get("non-existing-key"));

        final Map<String, AppDescriptor> meta2 = cache.getAppDescriptorsMap(null);
        Assert.assertEquals(meta2.get("k1").getName(),"k1");
        Assert.assertEquals(meta2.get("k2").getName(),"k2");
        Assert.assertNull(meta2.get("non-existing-key"));

        cache.invalidateDescriptorsCache();

        final Map<String, AppDescriptor> meta3 = cache.getAppDescriptorsMap(null);
        Assert.assertNull(meta3);

    }

    /**
     * Short hand utility method to create AppDescriptor with semi-arbitrary values.
     * @param key
     * @return
     */
    private AppDescriptor createDescriptor(final String key){
        return new AppDescriptorImpl(key + ".yml", false, key, key,null,false, null);
    }
}
