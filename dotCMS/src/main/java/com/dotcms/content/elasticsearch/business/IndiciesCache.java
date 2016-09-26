package com.dotcms.content.elasticsearch.business;

import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotmarketing.business.Cachable;

public interface IndiciesCache extends Cachable {
    public IndiciesInfo get();
    public void put(IndiciesInfo info);
}
