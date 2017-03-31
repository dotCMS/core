package com.dotmarketing.business.cache.provider.h22;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.repackage.org.apache.commons.lang.RandomStringUtils;
import com.dotcms.repackage.org.apache.log4j.BasicConfigurator;
import com.dotcms.repackage.org.apache.log4j.Logger;
import com.dotcms.repackage.org.apache.logging.log4j.Level;
import com.dotcms.repackage.org.apache.logging.log4j.LogManager;
import com.dotcms.repackage.org.apache.logging.log4j.core.LoggerContext;
import com.dotcms.repackage.org.apache.logging.log4j.core.appender.ConsoleAppender;
import com.dotcms.repackage.org.apache.logging.log4j.core.config.AbstractConfiguration;
import com.dotcms.repackage.org.apache.logging.log4j.core.config.AppenderRef;
import com.dotcms.repackage.org.apache.logging.log4j.core.config.LoggerConfig;
import com.liferay.util.FileUtil;

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
	final int numberOfPuts = 100000;
	final int numberOfThreads = 20;
	final int numberOfGroups = 100;
	final int maxCharOfObjects = 10000;
	private static final Logger LOGGER = Logger.getLogger(H22CacheTest.class);
	


	@Test
	public void testInit() throws Exception {
		  LOGGER.info("INFO TEST");
	       LOGGER.debug("DEBUG TEST");
	       LOGGER.error("ERROR TEST");
		// File dir = Files.createTempDir();
		File dir = new File("/tmp/h2cachetest");
		dir.delete();
		dir.mkdirs();

		H22Cache cache = new H22Cache(dir.getCanonicalPath());
		cache.init();

		
		
		assertThat("Are we the H2 Cache Loader?", "H22Cache".equals(cache.getKey()));

		for (String group : GROUPNAMES) {
			// put content
			cache.put(group, KEYNAME, CONTENT);
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

		// try to cache something that can't be cached
		cache.put(LONG_GROUPNAME, CANT_CACHE_KEYNAME, new CantCacheMeObject());
		assertThat("we should be null because of the CANT_CACHE_ME ", cache.get(LONG_GROUPNAME, CANT_CACHE_KEYNAME) == null);

		// try to cache a log key
		cache.put(LONG_GROUPNAME, LONG_KEYNAME, CONTENT);
		assertThat("We should cache with a long key", CONTENT.equals(cache.get(LONG_GROUPNAME, LONG_KEYNAME)));

		Fqn fqn = new Fqn(LONG_GROUPNAME, LONG_KEYNAME);
		Set<String> keys = cache.getKeys(LONG_GROUPNAME);
		assertThat("Keys should include long key", keys.contains(fqn.id));

		assertThat("Cache not flushed , we have groups", cache.getGroups().size() > 0);
		// Flush all caches
		cache.removeAll();
		assertThat("Cache flushed, we have no groups", cache.getGroups().size() == 0);


		
		// test dumping and rebuilding cache
		//testMultithreaded(cache, numberOfThreads,true, false, true);
		
		// test causing an error and recovering
		testMultithreaded(cache, numberOfThreads,false, true, false);
		
		//
		cache.shutdown();

		// cleanup
		FileUtil.deltree(dir);

	}

	void testMultithreaded(H22Cache cache, int numberOfThreads,boolean dumpCacheInMiddle, boolean breakCache, final boolean dieOnError) throws InterruptedException, SQLException{

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
			if(errors.size()>0 && dieOnError){
				throw new AssertionError(errors.get(0)) ;
			}
			Thread.sleep(500);
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
		assertThat("Cache filled , we should have 100 groups", cache.getGroups().size() == numberOfGroups);
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
		Optional<Connection> conn = cache.createConnection(true, 0);
		if(conn.isPresent()){
			Statement stmt = conn.get().createStatement();
			stmt.execute("drop table " + H22Cache.TABLE_PREFIX + "0");
			conn.get().close();
		}
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

			String group = "group_" + iter % numberOfGroups;
			String key = RandomStringUtils.randomAlphanumeric(20);
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
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {

				}
				
				cache.put(group, key, val);
				newVal = (String) cache.get(group, key);

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
