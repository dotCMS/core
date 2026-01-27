package com.dotcms.analytics.web;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotcms.experiments.business.ConfigExperimentUtil;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.Secret;
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
import com.liferay.util.StringPool;
import com.liferay.util.Xss;
import io.vavr.Lazy;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
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
        this(new AtomicBoolean(Config.getBooleanProperty(ANALYTICS_AUTO_INJECT_TURNED_ON_KEY, false)), // injection turn on by default
                WebAPILocator.getHostWebAPI(), APILocator.getAppsAPI(),
                APILocator::systemUser, currentHost-> ContentAnalyticsUtil.getSiteKeyFromAppSecrets(currentHost).orElse(StringPool.BLANK));
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

        return Try.of(() -> this.appsAPI.getSecrets(
                        ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY, true, host, systemUserSupplier.get()).isPresent())
                .getOrElseGet(e -> {
                    Logger.warn(this, "Error getting analytics secrets. Please check that the App Content Analytics is configured: " + e.getMessage() + " for the site: " + host.getHostname(), e);
                    return false;
                });
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
     * Replaces template placeholders:
     * - ${siteAuth}: Analytics site key from the current host → data-analytics-auth
     * - ${debug}: Debug mode flag (default: false) → data-analytics-debug
     * - ${autoPageView}: Auto page view tracking flag (default: true) → data-analytics-auto-page-view
     *
     * @param currentHost Host to use the {@link com.dotcms.analytics.app.AnalyticsApp}
     * @param request To get the Domain name
     * @return The processed Analytics JS code with placeholders replaced
     */
    private String getJSCode(final Host currentHost, final HttpServletRequest request) {

        try {

            final StringBuilder builder = new StringBuilder(this.jsCode.get());
            final Map<String, Secret> secrets = ContentAnalyticsUtil.getAppSecrets(currentHost);

            final Function<String, String> getSecret = (key) -> {
                final Secret secret = secrets.get(key);
                return (secret != null) ? secret.getString() : StringPool.BLANK;
            };

            final Map<String, String> placeholders = new HashMap<>();
            // Escape user-provided values to prevent XSS attacks
            placeholders.put("${siteAuth}", Xss.escapeHTMLAttrib(getSecret.apply("siteAuth")));
            placeholders.put("${debug}", getSecret.apply("debug"));
            placeholders.put("${autoPageView}", getSecret.apply("autoPageView"));
            placeholders.put("${contentImpression}", getSecret.apply("contentImpression"));
            placeholders.put("${contentClick}", getSecret.apply("contentClick"));
            placeholders.put("${advancedConfig}", Xss.escapeHTMLAttrib(getSecret.apply("advancedConfig")));

            placeholders.forEach((key, value) -> {

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
