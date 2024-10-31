package com.dotcms.telemetry.collectors.sitesearch;

import com.dotcms.content.elasticsearch.business.IndexStats;
import com.dotmarketing.exception.DotDataException;
import org.elasticsearch.common.unit.ByteSizeValue;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Collect the total size in Mb of all the Site Search indices.
 */
public class TotalSizeSiteSearchIndicesMetricType extends IndicesSiteSearchMetricType {


    @Override
    public String getName() {
        return "TOTAL_INDICES_SIZE";
    }

    @Override
    public String getDescription() {
        return "Total size of indexes";
    }

    @Override
    public Optional<Object> getValue(Collection<IndexStats> indices) throws DotDataException {
        return Optional.of(new ByteSizeValue(
                indices.stream().collect(Collectors.summingLong(IndexStats::getSizeRaw))).toString()
        );
    }
}
