package com.dotcms.telemetry.util;

import com.dotcms.content.elasticsearch.business.IndexStats;
import com.dotcms.content.elasticsearch.business.IndexType;
import com.dotmarketing.business.APILocator;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provide util methods to get data from the SiteSearch Indices
 */
public enum IndicesSiteSearchUtil {

    INSTANCE;

    /**
     * Return all the Site Search Index Information.
     *
     * @return
     */
    public Collection<IndexStats> getESIndices() {
        return APILocator.getESIndexAPI().getIndicesStats().entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(index -> IndexType.SITE_SEARCH.is(index.getIndexName()))
                .collect(Collectors.toList());
    }

}
