package com.dotmarketing.business.cache.provider.hazel;

import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;
import com.dotcms.repackage.org.apache.commons.lang.RandomStringUtils;
import com.dotcms.repackage.org.apache.log4j.Logger;
import com.dotmarketing.business.cache.provider.hazelcast.HazelCastEmbeddedProvider;
import com.hazelcast.nio.serialization.HazelcastSerializationException;


public class HazelCacheTest {

  final String[] GROUPNAMES = {"testGroup", "testGroup2", "myBigGroup"};
  final String KEYNAME = "testKey";
  final String CONTENT = "test my Content!!!";
  final String LONG_GROUPNAME = "thifsais-as-disadisaidsadiias-dsaidisadisaid";
  final String LONG_KEYNAME = new String("A" + "\u00ea" + "\u00f1" + "\u00fc" + "C"
      + "ASDSADFDDSFasfddsadasdsadsadasdq3r4efwqrrqwerqewrqewrqreqwreqwrqewrqwerqewrqewrqwerqwerqwerqewr43545324542354235243524354325423qwerewds fds fds gf eqgq ewg qeg qe wg egw  ww  eeR ASdsadsadadsadsadsadaqrewq43223t14@#$#@^%$%&%#$sfwf erqwfewfqewfgqewdsfqewtr243fq43f4q444fa4ferfrearge");;
  final String CANT_CACHE_KEYNAME = "CantCacheMe";
  final int numberOfPuts = 100000;
  final int numberOfThreads = 20;
  final int numberOfGroups = 100;
  final int maxCharOfObjects = 10000;
  private static final Logger LOGGER = Logger.getLogger(HazelCacheTest.class);



  @Test
  public void testInit() throws Exception {
    LOGGER.info("INFO TEST");
    LOGGER.debug("DEBUG TEST");
    LOGGER.error("ERROR TEST");


    HazelCastEmbeddedProvider cache = new HazelCastEmbeddedProvider();
    cache.init();



    assertThat("Are we the HazelCastEmbeddedProvider Cache Loader?",
        "HazelCastEmbeddedProvider".equals(cache.getKey()));

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
    boolean works = false;
    try {
      cache.put(LONG_GROUPNAME, CANT_CACHE_KEYNAME, new CantCacheMeObject());
    } catch (HazelcastSerializationException hse) {
      works = true;
    }
    assertThat("Only serializable objects should work", works);

    assertThat("we should be null because of the CANT_CACHE_ME ",
        cache.get(LONG_GROUPNAME, CANT_CACHE_KEYNAME) == null);

    // try to cache a log key
    cache.put(LONG_GROUPNAME, LONG_KEYNAME, CONTENT);
    assertThat("We should cache with a long key", CONTENT.equals(cache.get(LONG_GROUPNAME, LONG_KEYNAME)));


    assertThat("Cache not flushed , we have groups", cache.getGroups().size() > 0);
    // Flush all caches
    cache.removeAll();
    // assertThat("Cache flushed, we have no groups", cache.getGroups().size() == 0);



    // test dumping and rebuilding cache
    // testMultithreaded(cache, numberOfThreads,true, false, true);

    // test causing an error and recovering
    testMultithreaded(cache, numberOfThreads, false, true, false);

    //
    cache.shutdown();


  }

  void testMultithreaded(HazelCastEmbeddedProvider cache, int numberOfThreads, boolean dumpCacheInMiddle,
      boolean breakCache, final boolean dieOnError) throws InterruptedException, SQLException {

    ExecutorService pool = Executors.newFixedThreadPool(numberOfThreads);
    int oldSize = cache.getGroups().size();

    final List<Throwable> errors = new ArrayList<>();
    for (int i = 0; i < numberOfPuts; i++) {
      pool.execute(new TestRunner(cache, i, errors));
    }
    pool.shutdown();


    int size = cache.getGroups().size();
    for (int i = 0; i < 20; i++) {
      if (size < numberOfGroups) {
        Thread.sleep(1000);
        size = cache.getGroups().size();
      } else {
        break;
      }
    }
    assertThat("Cache filled , we should have 100 groups", cache.getGroups().size() == numberOfGroups + oldSize);
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
    assertThat("Cache with 1 group removed , we should have 99 groups",
        cache.getGroups().size() == (numberOfGroups - 1));
    // assertThat("Cache filled , we should have 10 in group 1", cache.getKeys("group_1").size() ==
    // numberOfPuts / numberOfGroups);

  }

  void breakCache(HazelCastEmbeddedProvider cache) throws SQLException {

  }


  class TestRunner implements Runnable {

    final HazelCastEmbeddedProvider cache;
    final int iter;
    final List<Throwable> errors;

    public TestRunner(HazelCastEmbeddedProvider cache, int iter, List<Throwable> errors) {
      this.cache = cache;
      this.iter = iter;
      this.errors = errors;
    }

    @Override
    public void run() {

      String group = "group_" + iter % numberOfGroups;
      String key = RandomStringUtils.randomAlphanumeric(20);
      int len = (int) (Math.random() * maxCharOfObjects);
      String val = RandomStringUtils.randomAlphanumeric(len + 1);
      cache.put(group, key, val);
      String newVal = (String) cache.get(group, key);

      assertThat("Test Cache hit" + group + "-" + key, val.equals(newVal));



    }
  };


  public final class CantCacheMeObject {
    final String notSerializable = "fail!";

  }

}
