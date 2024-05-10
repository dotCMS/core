package com.dotcms.experiments.business;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.util.Config;
import com.liferay.util.StringPool;
import graphql.VisibleForTesting;
import io.vavr.Lazy;

import java.util.concurrent.TimeUnit;


/**
 * This is a Wrapper to check all the Configuration values needed to handle {@link com.dotcms.experiments.model.Experiment}.
 * Also it provide method to set these values to Testing Environment
 */
public enum ConfigExperimentUtil {

    INSTANCE;

    private Lazy<Boolean> isExperimentEnabled =
            Lazy.of(() -> Config.getBooleanProperty("FEATURE_FLAG_EXPERIMENTS", true));

    private Lazy<Boolean> isExperimentAutoJsInjection =
            Lazy.of(() -> Config.getBooleanProperty("ENABLE_EXPERIMENTS_AUTO_JS_INJECTION", false));


    /**
     * Set the FEATURE_FLAG_EXPERIMENTS FLAG into a Testing Environment
     * @param enabled
     */
    @VisibleForTesting
    public void setExperimentEnabled(final boolean enabled) {
        this.isExperimentEnabled =  Lazy.of(() -> enabled);
    }

    /**
     * Set the ENABLE_EXPERIMENTS_AUTO_JS_INJECTION FLAG into a Testing Environment
     * @param enabled
     */
    @VisibleForTesting
    public void setExperimentAutoJsInjection(final boolean enabled) {
        this.isExperimentAutoJsInjection =  Lazy.of(() -> enabled);
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
        return this.isExperimentAutoJsInjection.get();
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
        return this.isExperimentEnabled.get();
    }


    /**
     * Return the Default lookBackWindow expire time in millis
     *
     * @return
     */
    public long lookBackWindowDefaultExpireTime() {
        return TimeUnit.DAYS.toMillis(ExperimentsAPI.EXPERIMENT_LOOKBACK_WINDOW.get());
    }


    public String getAnalyticsKey(Host host) {
        try {
            final AnalyticsApp analyticsApp = AnalyticsHelper.get().appFromHost(host);
            return analyticsApp.getAnalyticsProperties().analyticsKey();
        } catch (IllegalStateException e) {
            return StringPool.BLANK;
        }
    }
}
