package com.dotcms.cache;


import com.dotcms.rest.api.v1.HTTPMethod;

/**
 * Factory class to return the proper {@link DotJSONCache} implementation based on the used {@link HTTPMethod}
 */

public final class DotJSONCacheFactory {

    private DotJSONCacheFactory() {}

    public static DotJSONCache getCache(final HTTPMethod httpMethod) {
        if(httpMethod.equals(HTTPMethod.GET)) {
            return new DotJSONCacheImpl();
        } else {
            return new NoDotJSONCache();
        }
    }
}
