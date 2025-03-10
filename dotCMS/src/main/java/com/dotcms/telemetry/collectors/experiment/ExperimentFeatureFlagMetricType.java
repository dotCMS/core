package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricType;
import com.dotmarketing.util.Config;

import java.util.Optional;

/**
 * Returns true if the feature flag is enabled for the experiment
 * @author jsanca
 */
public class ExperimentFeatureFlagMetricType implements MetricType {


    @Override
    public String getName() {
        return "EXPERIMENT_FEATURE_FLAG_ON";
    }

    @Override
    public String getDescription() {
        return "Says if the feature flag for the experiments is enabled";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.PAID_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.EXPERIMENTS;
    }

    @Override
    public Optional<Object> getValue() {
        return Optional.of(Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_EXPERIMENTS, false));
    }
}
