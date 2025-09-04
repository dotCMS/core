package com.dotcms.analytics.web;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotcms.experiments.business.ConfigExperimentUtil;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.security.apps.AppsAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import io.vavr.Lazy;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Default implementation of the AnalyticsWebAPI
 * @author jsanca
 */
public class AnalyticsWebAPIImpl implements AnalyticsWebAPI {

    private static final  String ANALYTICS_JS_CODE_CLASS_PATH = "/ca/html/analytics_head.html";
    private static final  String ANALYTICS_AUTO_INJECT_TURNED_ON_KEY = FeatureFlagName.FEATURE_FLAG_CONTENT_ANALYTICS_AUTO_INJECT;
    private final AtomicBoolean isAutoInjectTurnedOn;
    private final HostWebAPI hostWebAPI;
    private final AppsAPI appsAPI;
    private final Supplier<User> systemUserSupplier;
    private final Lazy<String> jsCode;
    private final Function<Host,String> analyticsKeyFunction;

    public AnalyticsWebAPIImpl() {
        this(new AtomicBoolean(Config.getBooleanProperty(ANALYTICS_AUTO_INJECT_TURNED_ON_KEY, true)), // injection turn on by default
                WebAPILocator.getHostWebAPI(), APILocator.getAppsAPI(),
                APILocator::systemUser, currentHost-> String.valueOf(ContentAnalyticsUtil.getSiteKeyFromAppSecrets(currentHost)));
    }

    public AnalyticsWebAPIImpl(final AtomicBoolean isAutoInjectTurnedOn,
                               final HostWebAPI hostWebAPI,
                               final AppsAPI appsAPI,
                               final Supplier<User> systemUserSupplier,
                               final Function<Host,String> analyticsKeyFunction) {
        this.isAutoInjectTurnedOn = isAutoInjectTurnedOn;
        this.hostWebAPI = hostWebAPI;
        this.appsAPI = appsAPI;
        this.systemUserSupplier = systemUserSupplier;
        this.analyticsKeyFunction = analyticsKeyFunction;
        this.jsCode = Lazy.of(() -> FileUtil.toStringFromResourceAsStreamNoThrown(ANALYTICS_JS_CODE_CLASS_PATH));
    }

    @Override
    public boolean isAutoJsInjectionEnabled(final HttpServletRequest request) {

        return this.isAutoJsInjectionFlagOn() && anyAnalyticsConfig(request);
    }

    @Override
    public boolean isAutoJsInjectionFlagOn() {
        return this.isAutoInjectTurnedOn.get();
    }

    @Override
    public  boolean anyAnalyticsConfig(final HttpServletRequest request) {

        final Host currentSite = this.hostWebAPI.getCurrentHostNoThrow(request);

        return anySecrets(currentSite);
    }

    /**
     * Returns true if the host or the system host has any secrets for the analytics app.
     * @param host
     * @return
     */
    private boolean anySecrets (final Host host) {

        return   Try.of(
                        () ->
                                this.appsAPI.getSecrets(
                                        AnalyticsApp.ANALYTICS_APP_KEY, true, host, systemUserSupplier.get()).isPresent())
                .getOrElseGet(e -> false);
    }

    @Override
    public Optional<String> getCode(final Host host, final HttpServletRequest request) {

        if (PageMode.get(request) == PageMode.LIVE) {

            try {
                return Optional.ofNullable(getJSCode(hostWebAPI.getCurrentHostNoThrow(request), request));
            } catch (Exception e) {
                Logger.error(this, "It is not possible to generate the Analytics JS Code:" + e.getMessage());
            }
        }

        return Optional.empty();
    }

    /**
     * Return the Analytics Js Code to inject
     *
     * @param currentHost Host to use the {@link com.dotcms.analytics.app.AnalyticsApp}
     * @param request To get the Domain name
     * @return
     */
    private String getJSCode(final Host currentHost, final HttpServletRequest request) {

        try {

            final StringBuilder builder = new StringBuilder(this.jsCode.get());

            Map.of("${jitsu_key}", this.analyticsKeyFunction.apply(currentHost))
                    .forEach((key, value) -> {

                int start;
                while ((start = builder.indexOf(key)) != -1) {
                    builder.replace(start, start + key.length(), value);
                }
            });

            return builder.toString();
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public void notify(final SystemTableUpdatedKeyEvent event) {

        if (event.getKey().contains(ANALYTICS_AUTO_INJECT_TURNED_ON_KEY)) {
            isAutoInjectTurnedOn.set(Config.getBooleanProperty(ANALYTICS_AUTO_INJECT_TURNED_ON_KEY, true));
        }
    }
}
