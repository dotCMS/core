package com.dotcms.telemetry.collectors.sitesearch;

import com.dotcms.content.elasticsearch.business.IndexStats;
import com.dotmarketing.exception.DotDataException;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;

/**
 * Collects the number of documents in all site search indexes.
 */
@MetricsProfile(ProfileType.FULL)
@ApplicationScoped
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
