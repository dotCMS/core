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

    private final AtomicBoolean featureFlagExperiments;
    private final AtomicBoolean enableExperimentsAutoJsInjection;

    ConfigExperimentUtil() {
        featureFlagExperiments = new AtomicBoolean(resolveFeatureFlag());
        enableExperimentsAutoJsInjection = new AtomicBoolean(resolveEnableAutoJsInjection());
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
     * Return true if the FEATURE_FLAG_EXPERIMENTS is set to true, this mean that
     * we are going to support Experiment features.
     *
     * The default value is FALSE
     *
     * @return
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

    @Override
    public void notify(final SystemTableUpdatedKeyEvent event) {
        if (event.getKey().contains(FEATURE_FLAG_EXPERIMENTS_KEY)) {
            featureFlagExperiments.set(resolveFeatureFlag());
        } else if (event.getKey().contains(ENABLE_EXPERIMENTS_AUTO_JS_INJECTION_KEY)) {
            enableExperimentsAutoJsInjection.set(resolveEnableAutoJsInjection());
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
        return Config.getBooleanProperty(FEATURE_FLAG_EXPERIMENTS_KEY, true);
    }

    private boolean resolveEnableAutoJsInjection() {
        return Config.getBooleanProperty(ENABLE_EXPERIMENTS_AUTO_JS_INJECTION_KEY, false);
    }
}
