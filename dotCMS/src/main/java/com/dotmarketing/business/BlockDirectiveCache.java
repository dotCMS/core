package com.dotmarketing.business;

public abstract class BlockDirectiveCache implements Cachable {

  public abstract void add(String key, String val, int ttl);

  public abstract String get(String key, int ttl);

  public abstract BlockDirectiveCacheObject get(String key);

  public abstract void clearCache();

  public abstract void remove(String key);
}
