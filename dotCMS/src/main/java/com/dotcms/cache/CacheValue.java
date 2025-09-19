package com.dotcms.cache;

import java.io.Serializable;

public interface CacheValue extends Serializable {


    public Object getValue();

    public long getTtlInMillis();

    public boolean isExpired();

}
