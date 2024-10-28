package com.dotcms.experience.collectors.sitesearch;

import com.dotcms.content.elasticsearch.business.IndexStats;
import com.dotmarketing.exception.DotDataException;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Collects the number of documents in all site search indexes.
 */
public class CountSiteSearchDocumentMetricType extends IndicesSiteSearchMetricType {


    @Override
    public String getName() {
        return "INDICES_DOCUMENT_COUNT";
    }

    @Override
    public String getDescription() {
        return "Number of documents in indexes";
    }

    @Override
    public Optional<Object> getValue(Collection<IndexStats> indices) throws DotDataException {
        return Optional.of(indices.stream()
                .collect(Collectors.summingLong(IndexStats::getDocumentCount))
        );
    }
}
