package com.dotcms.cache;

import com.dotcms.rest.api.v1.vtl.VTLResource.HTTPMethod;

public class DotJSONCacheStrategyFactory {
    public static DotJSONCacheStrategy getCacheStrategy(final HTTPMethod httpMethod) {
        switch(httpMethod) {
            case GET:
                return new DotJSONCacheStrategyImpl();
            default:
                return new DotJSONNoCacheStrategyImpl();
        }
    }
}
