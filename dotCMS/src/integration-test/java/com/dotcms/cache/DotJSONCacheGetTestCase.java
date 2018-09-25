package com.dotcms.cache;

import com.dotcms.api.vtl.model.DotJSON;

public class DotJSONCacheGetTestCase {
    private final DotJSON dotJSONToAdd;
    private final String cacheKeyToAdd;
    private final String cacheKeyToGet;
    private final long waitTime;
    private final DotJSON dotJSONToExpect;

    public DotJSONCacheGetTestCase(final DotJSON dotJSONToAdd, final String cacheKeyToAdd, final String cacheKeyToGet,
                                   final long waitTime,
                                   final DotJSON dotJSONToExpect) {
        this.dotJSONToAdd = dotJSONToAdd;
        this.cacheKeyToAdd = cacheKeyToAdd;
        this.cacheKeyToGet = cacheKeyToGet;
        this.waitTime = waitTime;
        this.dotJSONToExpect = dotJSONToExpect;
    }

    public DotJSON dotJSONToAdd() {
        return dotJSONToAdd;
    }

    public String cacheKeyToAdd() {
        return cacheKeyToAdd;
    }

    public String cacheKeyToGet() {
        return cacheKeyToGet;
    }

    public long waitTime() {
        return waitTime;
    }

    public DotJSON dotJSONToExpect() {
        return dotJSONToExpect;
    }

    public static class Builder {
        private DotJSON dotJSONToAdd;
        private String cacheKeyToAdd;
        private String cacheKeyToGet;
        private long waitTime;
        private DotJSON dotJSONToExpect;

        public Builder dotJSONToAdd(final DotJSON dotJSONToAdd) {
            this.dotJSONToAdd = dotJSONToAdd;
            return this;
        }

        public Builder cacheKeyToAdd(final String cacheKeyToAdd) {
            this.cacheKeyToAdd = cacheKeyToAdd;
            return this;
        }

        public Builder cacheKeyToGet(final String cacheKeyToGet) {
            this.cacheKeyToGet = cacheKeyToGet;
            return this;
        }

        public Builder waitTime(final long waitTime) {
            this.waitTime = waitTime;
            return this;
        }

        public Builder dotJSONToExpect(final DotJSON dotJSONToExpect) {
            this.dotJSONToExpect = dotJSONToExpect;
            return this;
        }

        public DotJSONCacheGetTestCase build() {
            return new DotJSONCacheGetTestCase(dotJSONToAdd, cacheKeyToAdd, cacheKeyToGet, waitTime, dotJSONToExpect);
        }
    }
}
