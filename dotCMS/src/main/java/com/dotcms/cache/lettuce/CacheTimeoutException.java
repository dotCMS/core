package com.dotcms.cache.lettuce;

import io.lettuce.core.RedisCommandTimeoutException;

/**
 * Exception when there is a timeout on redis
 * @author jsanca
 */
public class CacheTimeoutException extends RuntimeException {

    public CacheTimeoutException(final RedisCommandTimeoutException e) {
        super(e);
    }
}
