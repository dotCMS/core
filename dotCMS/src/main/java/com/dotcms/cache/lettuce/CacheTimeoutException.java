package com.dotcms.cache.lettuce;

import io.lettuce.core.RedisCommandTimeoutException;

public class CacheTimeoutException extends RuntimeException {

    public CacheTimeoutException(final RedisCommandTimeoutException e) {
        super(e);
    }
}
