package com.dotcms.telemetry.collectors.ai;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UtilMethods;

import java.util.Map;
import java.util.Optional;

public class TotalEmbeddingsIndexesMetricType implements MetricType {

    @Override
    public String getName() {
        return "TOTAL_EMBEDDINGS_INDEXES";
    }

    @Override
    public String getDescription() {
        return "Total number of Embeddings/Indexes in dotAI";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.AI;
    }

    @Override
    public Optional<Object> getValue() {
        final Map<String, Map<String, Object>> indexCountData = APILocator.getDotAIAPI().getEmbeddingsAPI().countEmbeddingsByIndex();
        return Optional.of(UtilMethods.isSet(indexCountData) ? indexCountData.size() : 0);
    }
}
