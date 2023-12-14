package com.dotcms.analytics.experience.metric;

import com.dotcms.analytics.experience.metric.collector.MetricCollectorType;
import com.dotcms.util.JsonUtil;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a Metrics Results Summary
 */
public class MetricResult implements Serializable {

    private MetricCategoryResult metricCategoryResult;

    public MetricResult(final Builder builder) {
        this.metricCategoryResult = builder.result;
    }

    public MetricCategoryResult getMetricCategoryResult() {
        return metricCategoryResult;
    }

    @Override
    public String toString(){
        return JsonUtil.getJsonStringFromObject(metricCategoryResult.result);
    }

    public static class Builder {
        private MetricCategoryResult result = new MetricCategoryResult();

        public MetricFeatureResult get(final MetricCategory metricCategory){
            return result.get(metricCategory);
        }

        public MetricResult build(){
            return new MetricResult(this);
        }
    }

    static final class MetricCategoryResult implements Serializable {
        private Map<MetricCategory, MetricFeatureResult> result = new HashMap<>();

        public MetricFeatureResult get(final MetricCategory metricCategory){
            MetricFeatureResult featureResult = result.get(metricCategory);

            if (featureResult == null) {
                featureResult = new MetricFeatureResult();
                result.put(metricCategory, featureResult);
            }

            return featureResult;
        }

        @JsonAnyGetter
        public Map<MetricCategory, MetricFeatureResult> getResult() {
            return result;
        }
    }

    static final class MetricFeatureResult implements Serializable {
        private Map<MetricFeature, Result> result = new HashMap<>();

        public Result get(final MetricFeature metricFeature){
            Result metricResult = result.get(metricFeature);

            if (metricResult == null) {
                metricResult = new Result();
                result.put(metricFeature, metricResult);
            }

            return metricResult;
        }

        @JsonAnyGetter
        public Map<MetricFeature, Result> getResult() {
            return result;
        }
    }

    static final class Result implements Serializable {
        private Map<MetricCollectorType, Object> result = new HashMap<>();

        public void put(final MetricCollectorType metricCollectorType, final Object value){
            result.put(metricCollectorType, value);
        }

        @JsonAnyGetter
        public Map<MetricCollectorType, Object> getResult() {
            return result;
        }
    }
}
