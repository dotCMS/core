package com.dotcms.content.elasticsearch.business;

import com.dotmarketing.business.Cachable;

/**
 * IMPORTANT: This Is marked Deprecated and will be removed once we complete migration to OpenSearch 3.x
 * @deprecated Use {@link com.dotcms.content.index.VersionedIndicesAPI} instead.
 */
@Deprecated(forRemoval = true)
public interface IndiciesCache extends Cachable {
    public IndiciesInfo get();
    public void put(IndiciesInfo info);
}