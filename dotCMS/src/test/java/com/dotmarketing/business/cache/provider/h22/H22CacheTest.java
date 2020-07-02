package com.dotmarketing.business.cache.provider.h22;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.util.FileUtil;
import io.vavr.control.Try;

public class H22CacheTest {

	final String[] GROUPNAMES = { "testGroup", "testGroup2", "myBigGroup" };
	final String KEYNAME = "testKey";
	final String CONTENT = "test my Content!!!";
	final String LONG_GROUPNAME = "thifsais-as-disadisaidsadiias-dsaidisadisaid";
	final String LONG_KEYNAME = new String(
			"A"
					+ "\u00ea"
					+ "\u00f1"
					+ "\u00fc"
					+ "C"
					+ "ASDSADFDDSFasfddsadasdsadsadasdq3r4efwqrrqwerqewrqewrqreqwreqwrqewrqwerqewrqewrqwerqwerqwerqewr43545324542354235243524354325423qwerewds fds fds gf eqgq ewg qeg qe wg egw	ww	eeR ASdsadsadadsadsadsadaqrewq43223t14@#$#@^%$%&%#$sfwf	erqwfewfqewfgqewdsfqewtr243fq43f4q444fa4ferfrearge");;
	final String CANT_CACHE_KEYNAME = "CantCacheMe";
	final int numberOfPuts = 5000;
	final int numberOfThreads = 40;
	final int numberOfGroups = 100;
	final int maxCharOfObjects = 100;
    final private ExecutorService executor  = Executors.newSingleThreadExecutor();
    
    final public H22Cache cache = getCache();
    
    final H22Cache getCache() {
        final File dir = new File("/tmp/h2cachetest");
        dir.delete();
        dir.mkdirs();
        return new H22Cache(dir.getAbsolutePath());
        
    }
    
    
    @Before
    public void testInit() throws Exception {

        cache.init();
    
    }
    
    
	@Test
	public void test_h22_basic_cache_functions() throws Exception {

		assertThat("Are we the H2 Cache Loader?", "H22Cache".equals(cache.getKey()));

		for (String group : GROUPNAMES) {
			// put content
			cache.put(group, KEYNAME, CONTENT);
			// get content from cache
			assertThat("Did we cache something", CONTENT.equals(getFromCache(group, KEYNAME).get()));
			// flush the group and check that we are null
			cache.remove(group);
			assertThat("we should be null", cache.get(group, KEYNAME) == null);

			// put content
			cache.put(group, KEYNAME, CONTENT);

			// get content from cache
			assertThat("Did we cache something", CONTENT.equals(getFromCache(group, KEYNAME).get()));
			// remove the keyname
			cache.remove(group, KEYNAME);

			// get content from cache and check that we are null after remove
			assertThat("we should be null after remove", getFromCache(group, KEYNAME).get() == null);

		}
	}
    @Test
    public void test_long_key() throws Exception {

		// try to cache a log key
		cache.put(LONG_GROUPNAME, LONG_KEYNAME, CONTENT);
		assertThat("We should cache with a long key", CONTENT.equals(getFromCache(LONG_GROUPNAME, LONG_KEYNAME).get()));

		// try to remove the long key
		cache.remove(LONG_GROUPNAME);
		assertThat("we should be null after remove", cache.get(LONG_GROUPNAME, KEYNAME) == null);
    }
    
    @Test
    public void test_cannot_cache_object() throws Exception {
        // try to cache something that can't be cached
        cache.put(LONG_GROUPNAME, CANT_CACHE_KEYNAME, new CantCacheMeObject());
        assertThat("we should be null because of the CANT_CACHE_ME ", getFromCache(LONG_GROUPNAME, CANT_CACHE_KEYNAME).get() == null);

        
        
    }
    @Test
    public void test_get_keys_and_remove_all() throws Exception {
        String keyName = UUIDGenerator.generateUuid();
        cache.put(LONG_GROUPNAME, keyName, CONTENT);
        
        getFromCache(LONG_GROUPNAME, keyName).get();
        
		Fqn fqn = new Fqn(LONG_GROUPNAME, keyName);
		Set<String> keys = cache.getKeys(LONG_GROUPNAME);
		assertThat("Keys should include long key", keys.contains(fqn.id));

		assertThat("Cache not flushed , we have groups", cache.getGroups().size() > 0);
		// Flush all caches
		cache.removeAll();
		assertThat("Cache flushed, we have no groups", cache.getGroups().size() == 0);

    }
		
    @Test
    public void test_multithreaded_access() throws Exception {

        // test dumping and rebuilding cache
        //testMultithreaded(cache, numberOfThreads,true, false, true);
        
        // test causing an error and recovering
        testMultithreaded(false, false);

    }
    
    
    
    
    
    
    @Test
    public void test_breaking_cache_access() throws Exception {

		// test dumping and rebuilding cache
		//testMultithreaded(cache, numberOfThreads,true, false, true);
		
		// test causing an error and recovering
		testMultithreaded(false, true);

	}
    
    @Test
    public void test_dumpCacheInMiddle() throws Exception {

        // test dumping and rebuilding cache
        //testMultithreaded(cache, numberOfThreads,true, false, true);
        
        // test causing an error and recovering
        testMultithreaded(true, false);

    }
    
    
    
    @After
    public void test_shutdown() throws Exception {

        cache.shutdown();

    }
	
    public Future<String> getFromCache(final String group, final String key) {        
        return executor.submit(() -> {
            for(int i=0;i<10;i++) {
                String value = (String) cache.get(group, key);
                if(value!=null) {
                    return value;
                }
                Thread.sleep(100);
            }
            
            return null;
        });
    }
	
	
	
	

	void testMultithreaded(boolean dumpCacheInMiddle, boolean breakCache) throws InterruptedException, SQLException{
	    
		ExecutorService pool = Executors.newFixedThreadPool(numberOfThreads);


		final List<Throwable> errors = new ArrayList<>();
		for (int i = 0; i < numberOfPuts; i++) {
			//test breaking the db mid test
			if(i==numberOfPuts/4 && breakCache){
				breakCache(cache);
			}
			
			//test dumping the db mid test
			if(i==numberOfPuts/2 && dumpCacheInMiddle){
				cache.dispose(true);
			}
			pool.execute(new TestRunner(cache, i, errors));
			

		}
		pool.shutdown();

		while(!pool.isTerminated()){
			Thread.sleep(50);
		}
		

		int size = cache.getGroups().size();
		for(int i=0;i<20;i++){
			if(size<numberOfGroups){
				Thread.sleep(1000);
				size = cache.getGroups().size();
			}else{
				break;
			}
		}

		Logger.info(this, "Number of groups: " + cache.getGroups().size());
		assertEquals("Cache filled , we should have 100 groups", numberOfGroups,
				cache.getGroups().size());
		cache.remove("group_1");
		size = cache.getGroups().size();
		for(int i=0;i<20;i++){
			if(size==numberOfGroups){
				Thread.sleep(1000);
				size = cache.getGroups().size();
			}else{
				break;
			}
		}
		assertThat("Cache with 1 group removed , we should have 99 groups", cache.getGroups().size() == (numberOfGroups-1));
		//assertThat("Cache filled , we should have 10 in group 1", cache.getKeys("group_1").size() == numberOfPuts / numberOfGroups);

	}
	
	void breakCache(H22Cache cache) throws SQLException{
	    
	    File dbFolder = new File(cache.dbRoot);
	    File newFolder = new File(dbFolder.getParent(), UUIDGenerator.shorty());
	    dbFolder.renameTo(newFolder);
	    FileUtil.deltree(newFolder, true);

	}
	
	
	class TestRunner implements Runnable {

		final H22Cache cache;
		final int iter;
		final List<Throwable> errors;
		public TestRunner(H22Cache cache, int iter, List<Throwable> errors){
			this.cache=cache;
			this.iter = iter;
			this.errors = errors;
		}
		
		@Override
		public void run() {

			final String group = "group_" + iter % numberOfGroups;
			final String key = RandomStringUtils.randomAlphanumeric(20);
			int len = (int) (Math.random() * maxCharOfObjects);
			String val = RandomStringUtils.randomAlphanumeric(len+1);
			cache.put(group, key, val);
			String newVal = null;

			newVal = (String) cache.get(group, key);
			int testTimes=10;
			// if the put failed (because the cache was rebuilding
			// and initiing, it is possible to get a null value, so we try again
			//
			while(newVal==null && --testTimes > 0){

				Try.run(()->Thread.sleep(50));

				
				cache.put(group, key, val);
				newVal = Try.of(()->getFromCache(group, key).get()).getOrNull();

			}
			try{
				assertThat("Test Cache hit" + group + "-" + key,  val.equals(newVal));
			}
			catch(Throwable t){
				if(errors.size()>100){
					errors.add(null);
				}
				else{
					errors.add(t);
				}
			}


			
		}
	};
	
	
	public final class CantCacheMeObject {
		final String notSerializable = "fail!";

	}

}
