package com.dotcms.experiments.business;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.liferay.util.StringPool;
import graphql.VisibleForTesting;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is a Wrapper to check all the Configuration values needed to handle {@link com.dotcms.experiments.model.Experiment}.
 * Also, it provides method to set these values to Testing Environment
 */
public enum ConfigExperimentUtil implements EventSubscriber<SystemTableUpdatedKeyEvent> {

    INSTANCE;

    private static final String FEATURE_FLAG_EXPERIMENTS_KEY = FeatureFlagName.FEATURE_FLAG_EXPERIMENTS;
    private static final String ENABLE_EXPERIMENTS_AUTO_JS_INJECTION_KEY = "ENABLE_EXPERIMENTS_AUTO_JS_INJECTION";
    private static final String FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS_KEY = FeatureFlagName.FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS;

    private final AtomicBoolean featureFlagExperiments;
    private final AtomicBoolean enableExperimentsAutoJsInjection;
    private final AtomicBoolean caemExperimentResults;

    ConfigExperimentUtil() {
        featureFlagExperiments = new AtomicBoolean(resolveFeatureFlag());
        enableExperimentsAutoJsInjection = new AtomicBoolean(resolveEnableAutoJsInjection());
        caemExperimentResults = new AtomicBoolean(resolveCaemExperimentResults());
        APILocator.getLocalSystemEventsAPI().subscribe(SystemTableUpdatedKeyEvent.class, this);
    }

    /**
     * Set the FEATURE_FLAG_EXPERIMENTS FLAG into a Testing Environment
     * @param enabled
     */
    @VisibleForTesting
    public void setExperimentEnabled(final boolean enabled) {
        featureFlagExperiments.set(enabled);
    }

    /**
     * Set the ENABLE_EXPERIMENTS_AUTO_JS_INJECTION FLAG into a Testing Environment
     * @param enabled
     */
    @VisibleForTesting
    public void setExperimentAutoJsInjection(final boolean enabled) {
        enableExperimentsAutoJsInjection.set(enabled);
    }

    /**
     * Set the FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS FLAG into a Testing Environment
     * @param enabled
     */
    @VisibleForTesting
    public void setCaemExperimentResultsEnabled(final boolean enabled) {
        caemExperimentResults.set(enabled);
    }

    /**
     * Returns {@code true} when {@code FEATURE_FLAG_EXPERIMENTS} is set to {@code true}, enabling
     * full experiment functionality. When {@code false}, the system operates in limited mode —
     * one free experiment with a 10-day maximum duration.
     *
     * <p>The value is read once at startup from the system config and held in an
     * {@link java.util.concurrent.atomic.AtomicBoolean}. It is <em>not</em> refreshed at runtime;
     * a server restart is required for changes to take effect.
     *
     * <p>The default value is {@code false}.
     *
     * @return {@code true} if experiments are fully enabled; {@code false} for limited mode
     */
    public boolean isExperimentEnabled() {
        return featureFlagExperiments.get();
    }

    /**
     * Return true if the ENABLE_EXPERIMENTS_AUTO_JS_INJECTION is set to true, this mean that
     * we are going to inject the Experiment Code automatically in the render Page process.
     *
     * The default value is FALSE
     *
     * @return
     */
    public boolean isExperimentAutoJsInjection() {
        return enableExperimentsAutoJsInjection.get();
    }

    /**
     * Return true if the FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS is set to true, meaning that
     * experiment result queries will be served from the CAEM analytics backend instead of CubeJS.
     *
     * The default value is FALSE
     *
     * @return
     */
    public boolean isCaemExperimentResultsEnabled() {
        return caemExperimentResults.get();
    }

    @Override
    public void notify(final SystemTableUpdatedKeyEvent event) {
        // FEATURE_FLAG_EXPERIMENTS intentionally omitted: live-toggle removed in Oct 2026.
        // A server restart is required for changes to that flag to take effect (FR-010).
        if (event.getKey().contains(ENABLE_EXPERIMENTS_AUTO_JS_INJECTION_KEY)) {
            enableExperimentsAutoJsInjection.set(resolveEnableAutoJsInjection());
        } else if (event.getKey().contains(FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS_KEY)) {
            caemExperimentResults.set(resolveCaemExperimentResults());
        }
    }

    /**
     * Return the Default lookBackWindow expire time in millis
     *
     * @return
     */
    public long lookBackWindowDefaultExpireTime() {
        return TimeUnit.DAYS.toMillis(APILocator.getExperimentsAPI().getExperimentsLookbackWindow());
    }

    /**
     * Gets Analytics Key from Analytics App.
     *
     * @param host host associates to {@link AnalyticsApp}
     * @return analytics key
     */
    public String getAnalyticsKey(final Host host) {
        try {
            final AnalyticsApp analyticsApp = AnalyticsHelper.get().appFromHost(host);
            return analyticsApp.getAnalyticsProperties().analyticsKey();
        } catch (IllegalStateException e) {
            return StringPool.BLANK;
        }
    }

    private boolean resolveFeatureFlag() {
        return Config.getBooleanProperty(FEATURE_FLAG_EXPERIMENTS_KEY, false);
    }

    private boolean resolveEnableAutoJsInjection() {
        return Config.getBooleanProperty(ENABLE_EXPERIMENTS_AUTO_JS_INJECTION_KEY, false);
    }

    private boolean resolveCaemExperimentResults() {
        return Config.getBooleanProperty(FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS_KEY, false);
    }
}
