package com.dotmarketing.business;

import java.io.Serializable;
import java.time.Duration;
import java.util.Map;

public abstract class BlockDirectiveCache implements Cachable {


    public static final String PAGE_CONTENT_KEY = "PAGE_CONTENT_KEY";

    public abstract void add(String key, Map<String, Serializable> val, Duration ttl);

    public abstract Map<String, Serializable> get(String key);

    public abstract void remove(String key);

}
