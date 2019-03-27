package com.dotcms.cache;

import com.dotcms.api.vtl.model.DotJSON;

public class DotJSONCacheAddTestCase {
    private DotJSON dotJSON;
    private String cacheKey;
    private final boolean shouldCache;

    private DotJSONCacheAddTestCase(final DotJSON dotJSON, final String cacheKey,
                                    final boolean shouldCache) {
        this.dotJSON = dotJSON;
        this.cacheKey = cacheKey;
        this.shouldCache = shouldCache;
    }

    public DotJSON getDotJSON() {
        return dotJSON;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public boolean shouldCache() {
        return shouldCache;
    }

    public static class Builder {
        private DotJSON dotJSON;
        private String cacheKey;
        private boolean shouldCache;

        public Builder dotJSON(final DotJSON dotJSON) {
            this.dotJSON = dotJSON;
            return this;
        }

        public Builder cacheKey(final String cacheKey) {
            this.cacheKey = cacheKey;
            return this;
        }

        public Builder shouldCache(final boolean shouldCache) {
            this.shouldCache = shouldCache;
            return this;
        }

        public DotJSONCacheAddTestCase build() {
            return new DotJSONCacheAddTestCase(dotJSON, cacheKey, shouldCache);
        }
    }
}
