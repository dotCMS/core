package com.dotcms.telemetry.collectors.sitesearch;

import com.dotcms.content.elasticsearch.business.IndexStats;
import com.dotmarketing.exception.DotDataException;

import java.util.Collection;
import java.util.Optional;

/**
 * Collect the count of Site Search indices
 */
public class CountSiteSearchIndicesMetricType extends IndicesSiteSearchMetricType {


    @Override
    public String getName() {
        return "INDICES_COUNT";
    }

    @Override
    public String getDescription() {
        return "Count of indexes";
    }

    @Override
    public Optional<Object> getValue(Collection<IndexStats> indices) throws DotDataException {
        return Optional.of(indices.size());
    }
}
