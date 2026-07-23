package com.dotcms.cache.lettuce;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.Portlet;

/**
 * Test for {@link RedisCache}
 * @author jsanca
 */

public class LettuceCacheTest {
    
    
    static RedisCache cache = null;
    
    @BeforeClass
    public static void startup() throws Exception {
        IntegrationTestInitService.getInstance().init();
        cache = new RedisCache();
    }


    @Test
    public void test_cache_key_generation() {

        if (RedisClientFactory.getClient("cache").ping()) {
            final String prefix = cache.REDIS_PREFIX_KEY;
            assert (prefix != null && prefix.length() > 3);
            String group = "group";
            String key = "key";
            String cacheKey = cache.cacheKey(group, key);
            assert (cacheKey != null);
            assert (cacheKey.equals(prefix + "." + group + "." + key));

            String cacheKey2 = cache.cacheKey(group, null);
            assert (cacheKey2 != null);
            assert (cacheKey2.equals(prefix + "." + group + "."));

            String cacheKey3 = cache.cacheKey(group);
            assert (cacheKey3 != null);
            assert (cacheKey3.startsWith(prefix));
            assert (cacheKey3.equals(cacheKey2));
        }
    }
    
    
    @Test
    public void test_cache_group_listing() {

        if (RedisClientFactory.getClient("cache").ping()) {
            long baseGroup = System.currentTimeMillis();
            cache.removeAll();
            String content = "my content";
            for (int i = 0; i < 5; i++) {
                cache.put("group" + baseGroup + i, "key" + i, content);
            }
            Set<String> groups = cache.getGroups();
            assert (groups.size() > 0);
            for (int i = 0; i < 5; i++) {
                assert (groups.contains("group" + baseGroup + i));
            }
        }
    }
    
    /**
     * Tests that listing keys work
     */
    @Test
    public void test_cache_key_listing() {

        if (RedisClientFactory.getClient("cache").ping()) {
            cache.removeAll();
            String group = "group";
            String key = "key";

            String content = "my content";
            for (int i = 0; i < 5; i++) {
                cache.put(group, key + i, content + i);
            }
            Set<String> keys = cache.getKeys(group);
            assert (keys.size() == 5);
            for (int i = 0; i < 5; i++) {
                Assert.assertTrue(keys.contains(key + i));
            }
        }
    }
    
    /**
     * Tests that flushall works
     */
    @Test
    public void test_flushall() {

        if (RedisClientFactory.getClient("cache").ping()) {
            String group = "group";
            String key = "key";
            cache.removeAll();
            String content = "my content";
            for (int i = 0; i < 1000; i++) {
                cache.put(group + i, key + i, content);
            }
            Set<String> keys = cache.getAllKeys();
            assert (keys.size() >= 1000);

            cache.removeAll();
            keys = cache.getAllKeys();
            assert (keys.isEmpty());
        }
    }

    /**
     * Tests that flushall works
     */
    @Test
    public void tests_puts_and_gets() {

        if (RedisClientFactory.getClient("cache").ping()) {
            String group = "group";
            String key = "key";
            String uuid = UUIDGenerator.generateUuid();
            cache.removeAll();
            final Contentlet con = new Contentlet();
            con.getMap().put("testing", uuid);
            con.setIdentifier(uuid);

            for (int i = 0; i < 10; i++) {
                cache.put(group + i, key + i, con);
            }

            for (int i = 0; i < 10; i++) {
                Contentlet returnedContentlet = (Contentlet) cache.get(group + i, key + i);

                assert (uuid.equals(con.getIdentifier()));
                assert (uuid.equals(con.getMap().get("testing")));
            }
        }
    }

    /**
     * Regression test for issue #34435: a {@link Portlet} cached in Redis is round-tripped through
     * {@code DotObjectCodec}'s Java serialization. The {@code initParams} field used to be
     * {@code transient}, so it came back {@code null} after a container restart and later NPE'd in
     * {@code PortletConfigImpl.getInitParameterNames()}. This asserts the init parameters survive the
     * real Redis put/get path (not just an in-memory ObjectStream round-trip).
     */
    @Test
    public void test_portlet_initParams_survive_redis_roundtrip() {

        if (RedisClientFactory.getClient("cache").ping()) {
            // Isolated test-only group so we never pollute the real "portletcache" namespace that
            // PortletCache and admin tooling use; RedisCache has no default TTL, so a leaked key
            // would persist indefinitely on the shared integration Redis.
            final String group = "portletcache_test_" + System.currentTimeMillis();
            final String key = "portlet" + System.currentTimeMillis();

            try {
                final Map<String, String> initParams = new HashMap<>();
                initParams.put("view-action", "/ext/contentlet/view_contentlets");
                initParams.put("name", "content");
                final Portlet portlet = new Portlet("content", "com.liferay.portlet.StrutsPortlet", initParams);

                cache.put(group, key, portlet);

                final Portlet cached = (Portlet) cache.get(group, key);
                Assert.assertNotNull("Portlet should be returned from Redis cache", cached);
                Assert.assertNotNull("initParams must survive the Redis serialization round-trip",
                        cached.getInitParams());
                Assert.assertEquals(initParams, cached.getInitParams());
            } finally {
                // Clean up even if an assertion above fails, so no stray key is left behind.
                cache.remove(group, key);
            }
        }
    }
}
