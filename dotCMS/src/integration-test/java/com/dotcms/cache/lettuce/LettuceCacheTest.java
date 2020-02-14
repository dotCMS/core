package com.dotcms.cache.lettuce;

import java.util.Set;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;

public class LettuceCacheTest {
    
    
    static LettuceCache cache = null;
    
    @BeforeClass
    public static void startup() throws Exception {
        cache = new  LettuceCache();
    }
    
    
    /**
     * all key values that are put into redis are prefixed by a random key.
     * When a flushall is called, this key is cycled, which basically invalidates
     * all cached entries.  The prefix key is stored in redis itself so multiple 
     * servers in the c
     */
    @Test
    public void test_prefix_key_cycling() {
       final String prefix1  = cache.loadPrefix();
       assert(prefix1!=null && prefix1.length()>3);
       
       cache.cycleKey();
       
       final String prefix2  = cache.loadPrefix();
       assert(prefix2!=null);
       
       // keys don't match after they have been cycled
       assert(!prefix1.equals(prefix2));
       
       
       // getting the key again and the keys match
       final String prefix3  = cache.loadPrefix();
       assert(prefix2.equals(prefix3));
       
    }

    
    
    @Test
    public void test_cache_key_generation() {
        
        final String prefix  = cache.loadPrefix();
        assert(prefix!=null && prefix.length()>3);
        String group = "group";
        String key = "key";
        String cacheKey = cache.cacheKey(group, key);
        assert (cacheKey!=null);
        assert (cacheKey.startsWith(prefix));
        assert (cacheKey.startsWith(prefix+ "." + group + "."));
        assert (cacheKey.equals(prefix+ "." + group+ "." + key));
        
        
        String cacheKey2 = cache.cacheKey(group, null);
        assert (cacheKey2!=null);
        assert (cacheKey2.startsWith(prefix));
        assert (cacheKey2.equals(prefix + "." + group + "."));

       
        String cacheKey3 = cache.cacheKey(group);
        assert (cacheKey3!=null);
        assert (cacheKey3.startsWith(prefix));
        assert (cacheKey3.equals(cacheKey2));
        
        
        
    }
    
    
    @Test
    public void test_cache_group_listing() {
        
        long baseGroup = System.currentTimeMillis();
        cache.removeAll();
        String content = "my content";
        for(int i=0;i<5;i++) {
            cache.put("group"+baseGroup+i, "key"+i, content);
        }
        Set<String> groups = cache.getGroups();
        assert(groups.size()>0);
        for(int i=0;i<5;i++) {
            assert(groups.contains("group"+baseGroup+i));
        }
    }
    
    /**
     * Tests that listing keys work
     */
    @Test
    public void test_cache_key_listing() {
        cache.removeAll();
        String group = "group";
        String key = "key";

        String content = "my content";
        for(int i=0;i<5;i++) {
            cache.put(group, key+i, content);
        }
        Set<String> keys = cache.getKeys(group);
        assert(keys.size()==5);
        for(int i=0;i<5;i++) {
            String cacheKey = cache.cacheKey(group, key+i);
            assert(keys.contains(cacheKey));
        }
    }
    
    /**
     * Tests that flushall works
     */
    @Test
    public void test_flushall() {
        String group = "group";
        String key = "key";
        cache.removeAll();
        String content = "my content";
        for(int i=0;i<1000;i++) {
            cache.put(group+i, key+i, content);
        }
        Set<String> keys = cache.getAllKeys();
        assert(keys.size()==1000);
        
        cache.removeAll();
        keys = cache.getAllKeys();
        assert(keys.isEmpty());
    }
    
    
    
    /**
     * Tests that flushall works
     */
    @Test
    public void tests_puts_and_gets() {
        String group = "group";
        String key = "key";
        String uuid = UUIDGenerator.generateUuid();
        cache.removeAll();
        final Contentlet con = new Contentlet();
        con.getMap().put("testing", uuid);
        con.setIdentifier(uuid);
        
        for(int i=0;i<10;i++) {
            cache.put(group+i, key+i, con);
        }

        for(int i=0;i<10;i++) {
            Contentlet returnedContentlet= (Contentlet) cache.get(group+i, key+i);
           
           assert(uuid.equals(con.getIdentifier()));
           assert(uuid.equals(con.getMap().get("testing")));
           
        }
        
        
        
    }
    
    
}
