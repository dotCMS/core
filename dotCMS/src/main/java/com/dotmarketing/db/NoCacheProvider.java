package com.dotmarketing.db;

import java.util.Properties;

import com.dotcms.repackage.net.sf.hibernate.cache.Cache;
import com.dotcms.repackage.net.sf.hibernate.cache.CacheException;
import com.dotcms.repackage.net.sf.hibernate.cache.CacheProvider;

public class NoCacheProvider implements CacheProvider {

  @Override
  public Cache buildCache(String arg0, Properties arg1) throws CacheException {
    throw new CacheException("No Cache Enabled");
  }

  @Override
  public long nextTimestamp() {
    return System.currentTimeMillis() / 100;
  }

  @Override
  public void start(Properties arg0) throws CacheException {


  }

  @Override
  public void stop() {


  }

}
