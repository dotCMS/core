package com.dotcms.content.elasticsearch.business;

import com.dotmarketing.business.Cachable;

public interface IndicesCache extends Cachable {
    LegacyIndicesInfo get();
    void put(LegacyIndicesInfo info);
}