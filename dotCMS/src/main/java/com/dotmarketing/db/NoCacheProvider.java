package com.dotmarketing.db;

import java.util.Properties;
import net.sf.hibernate.cache.Cache;
import net.sf.hibernate.cache.CacheException;
import net.sf.hibernate.cache.CacheProvider;

public class NoCacheProvider implements CacheProvider {

  @Override
  public Cache buildCache(String arg0, Properties arg1) throws CacheException {
    throw new CacheException("No Cache Enabled");
  }

  @Override
  public long nextTimestamp() {
    return System.currentTimeMillis() / 100;
  }

}
