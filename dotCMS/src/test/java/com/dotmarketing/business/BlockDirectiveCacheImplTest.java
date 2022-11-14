package com.dotmarketing.business;

import java.io.Serializable;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotmarketing.business.cache.provider.MockCacheAdministrator;

public class BlockDirectiveCacheImplTest {



    private static BlockDirectiveCacheImpl blockCacheAdmin;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        blockCacheAdmin = new BlockDirectiveCacheImpl(new MockCacheAdministrator(), true);

    }



    @Test
    public void test_cache() {
        blockCacheAdmin.clearCache();
        final String cacheKey = "cacheKey";
        final String now = "now:" + System.currentTimeMillis();
        Map<String,Serializable> cacheMap = Map.of(cacheKey,now );

        
        
        
        blockCacheAdmin.add(cacheKey, cacheMap, 0);
        
        
        
        
        
    }

}
