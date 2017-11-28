package com.dotmarketing.business.cache.provider.ignite;

import static org.hamcrest.MatcherAssert.assertThat;

import com.dotcms.repackage.org.apache.commons.lang.RandomStringUtils;
import com.dotcms.repackage.org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.ignite.internal.processors.cache.CacheStoppedException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class IgniteCacheTest {

    final static String[] GROUPNAMES = {"testGroup", "testGroup2", "myBigGroup"};
    final static String KEYNAME = "testKey";
    final static String CONTENT = "test my Content!!!";
    final static String LONG_GROUPNAME = "thifsais-as-disadisaidsadiias-dsaidisadisaid";
    final static String LONG_KEYNAME = new String("A" + "\u00ea" + "\u00f1" + "\u00fc" + "C"
            + "ASDSADFDDSFasfddsadasdsadsadasdq3r4efwqrrqwerqewrqewrqreqwreqwrqewrqwerqewrqewrqwerqwerqwerqewr43545324542354235243524354325423qwerewds fds fds gf eqgq ewg qeg qe wg egw	ww	eeR ASdsadsadadsadsadsadaqrewq43223t14@#$#@^%$%&%#$sfwf	erqwfewfqewfgqewdsfqewtr243fq43f4q444fa4ferfrearge");;
    final static String CANT_CACHE_KEYNAME = "CantCacheMe";
    final static int numberOfPuts = 5000;

    final static int numberOfGroups = 100;
    final static int maxCharOfObjects = 100;

    private static final Logger LOGGER = Logger.getLogger(IgniteCacheTest.class);

    private static IgniteCacheProvider cache = null;



    @BeforeClass
    public static void startup() throws Exception {

        cache = new IgniteCacheProvider("testing");
        cache.init();
        assertThat("Cache is  inited", cache.isInitialized());
        LOGGER.info("INFO TEST");
        LOGGER.debug("DEBUG TEST");
        LOGGER.error("ERROR TEST");
    }


    @AfterClass
    public static void shutdown() throws Exception {
        cache.removeAll();
        cache.shutdown();
        assertThat("Cache is not inited", !cache.isInitialized());
    }



    @Test
    public void testCorrectLoader() throws Exception {

        assertThat("Are we the Ignite Cache Loader?", "IgniteCacheProvider".equals(cache.getKey()));
    }



    @Test
    public void testPutsGets() throws Exception {



        for (String group : GROUPNAMES) {
            // put content
            cache.put(group, KEYNAME, CONTENT);
            
            
            String x = (String) cache.get(group, KEYNAME);
            // get content from cache
            assertThat("Did we cache something", CONTENT.equals(cache.get(group, KEYNAME)));
            // flush the group and check that we are null
            cache.remove(group);
            assertThat("we should be null", cache.get(group, KEYNAME) == null);

            // put content
            cache.put(group, KEYNAME, CONTENT);

            // get content from cache
            assertThat("Did we cache something", CONTENT.equals(cache.get(group, KEYNAME)));

            // remove the keyname
            cache.remove(group, KEYNAME);

            // get content from cache and check that we are null after remove
            assertThat("we should be null after remove", cache.get(group, KEYNAME) == null);

        }

        // try to cache a log key
        cache.put(LONG_GROUPNAME, LONG_KEYNAME, CONTENT);
        assertThat("We should cache with a long key", CONTENT.equals(cache.get(LONG_GROUPNAME, LONG_KEYNAME)));

        // try to remove the long key
        cache.remove(LONG_GROUPNAME);
        assertThat("we should be null after remove", cache.get(LONG_GROUPNAME, KEYNAME) == null);
        /*
         * // try to cache something that can't be cached cache.put(LONG_GROUPNAME,
         * CANT_CACHE_KEYNAME, new CantCacheMeObject());
         * 
         * assertThat("we should be null because of the CANT_CACHE_ME ", cache.get(LONG_GROUPNAME,
         * CANT_CACHE_KEYNAME) == null);
         */
        // try to cache a log key
        cache.put(LONG_GROUPNAME, LONG_KEYNAME, CONTENT);
        assertThat("We should cache with a long key", CONTENT.equals(cache.get(LONG_GROUPNAME, LONG_KEYNAME)));


        Set<String> keys = cache.getKeys(LONG_GROUPNAME);
        assertThat("Keys should include long key", keys.contains(LONG_KEYNAME));

        assertThat("Cache not flushed , we have groups", cache.getGroups().size() > 0);
        // Flush all caches
        cache.removeAll();
        assertThat("Cache flushed, we have no groups", cache.getGroups().size() == 0);



        // test dumping and rebuilding cache
        // testMultithreaded(cache, numberOfThreads,true, false, true);

    }

    @Test
    public void persistAcrossLoads() throws InterruptedException {
        cache.remove(GROUPNAMES[0]);
        cache.put(GROUPNAMES[0], KEYNAME, CONTENT);
        reinitCache();
        assertThat("Did we cache something", CONTENT.equals(cache.get(GROUPNAMES[0], KEYNAME)));
    }
    
    
    @Test
    public void testMultithreaded() throws InterruptedException {

        int numberOfThreads = 40;
        boolean breakCache = true;


        ExecutorService pool = Executors.newFixedThreadPool(numberOfThreads);


        final List<Throwable> errors = new ArrayList<>();
        for (int i = 0; i < numberOfPuts; i++) {
            // test breaking the db mid test
            if (i == numberOfPuts / 2 && breakCache) {
                reinitCache();
            }
            pool.execute(new TestRunner( i, errors));


        }
        pool.shutdown();

        while (!pool.isTerminated()) {
            if (errors.size() > 0) {
                throw new AssertionError(errors.get(0));
            }
            Thread.sleep(500);
        }


        int size = cache.getGroups().size();
        for (int i = 0; i < 20; i++) {
            if (size < numberOfGroups) {
                Thread.sleep(1000);
                size = cache.getGroups().size();
            } else {
                break;
            }
        }
        
        
        assertThat("Cache filled , we should have 100 groups", cache.getGroups().size() == numberOfGroups);
        cache.remove("group_1");
        size = cache.getGroups().size();
        for (int i = 0; i < 20; i++) {
            if (size == numberOfGroups) {
                Thread.sleep(1000);
                size = cache.getGroups().size();
            } else {
                break;
            }
        }
        assertThat("Cache with 1 group removed , we should have 99 groups", cache.getGroups().size() == (numberOfGroups - 1));
        // assertThat("Cache filled , we should have 10 in group 1", cache.getKeys("group_1").size()
        // == numberOfPuts / numberOfGroups);

    }

    void reinitCache()  {
        cache.shutdown();
        cache.init();
    }


    class TestRunner implements Runnable {


        final int iter;
        final List<Throwable> errors;

        public TestRunner(int iter, List<Throwable> errors) {

            this.iter = iter;
            this.errors = errors;
        }

        @Override
        public void run() {
            try {
                final String group = "group_" + iter % numberOfGroups;
                final String key = RandomStringUtils.randomAlphanumeric(20);
                final int len = (int) (Math.random() * maxCharOfObjects);
                final String val = RandomStringUtils.randomAlphanumeric(len + 1);
                cache.put(group, key, val);
                String newVal = null;

                newVal = (String) cache.get(group, key);
                int testTimes = 10;
                // if the put failed (because the cache was rebuilding
                // and initiing, it is possible to get a null value, so we try again
                //
                while (newVal == null && --testTimes > 0) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {

                    }

                    cache.put(group, key, val);
                    newVal = (String) cache.get(group, key);

                }

                assertThat("Test Cache hit" + group + "-" + key, val.equals(newVal));
            } catch (Throwable t) {
                
                Throwable cause = t.getCause();
                if(cause instanceof CacheStoppedException) {
                    return;
                }
                if (errors.size() > 100) {
                    errors.add(null);
                } else {
                    errors.add(t);
                }
            }



        }
    };


    public final class CantCacheMeObject {
        final String notSerializable = "fail!";

    }

}
