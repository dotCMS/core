package com.dotcms.analytics.track;

import com.dotcms.analytics.track.collectors.WebEventsCollectorServiceFactory;
import com.dotcms.analytics.track.matchers.FilesRequestMatcher;
import com.dotcms.analytics.track.matchers.PagesAndUrlMapsRequestMatcher;
import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotcms.analytics.track.matchers.VanitiesRequestMatcher;
import com.dotcms.analytics.web.AnalyticsWebAPI;
import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.WhiteBlackList;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.StringPool;
import io.vavr.Lazy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;

/**
 * Web Interceptor to track analytics
 * @author jsanca
 */
public class AnalyticsTrackWebInterceptor  implements WebInterceptor, EventSubscriber<SystemTableUpdatedKeyEvent> {

    private static final  String[] DEFAULT_BLACKLISTED_PROPS = new String[]{StringPool.BLANK};
    private static final  String ANALYTICS_TURNED_ON_KEY = FeatureFlagName.FEATURE_FLAG_CONTENT_ANALYTICS;
    private static final  Map<String, RequestMatcher> requestMatchersMap = new ConcurrentHashMap<>();
    private transient final AnalyticsWebAPI analyticsWebAPI;
    private final WhiteBlackList whiteBlackList;
    private final AtomicBoolean isTurnedOn;
    private final Lazy<WebEventsCollectorServiceFactory> webEventsCollectorServiceFactory;

    private static final String AUTO_INJECT_LIB_WEB_PATH = "/ext/analytics/ca.min.js";

    public AnalyticsTrackWebInterceptor() {

        this(new WhiteBlackList.Builder()
                        .addWhitePatterns(Config.getStringArrayProperty("ANALYTICS_WHITELISTED_KEYS",
                                new String[]{StringPool.BLANK})) // allows everything
                        .addBlackPatterns(CollectionsUtils.concat(Config.getStringArrayProperty(  // except this
                                "ANALYTICS_BLACKLISTED_KEYS", new String[]{}), DEFAULT_BLACKLISTED_PROPS)).build(),
                new AtomicBoolean(Config.getBooleanProperty(ANALYTICS_TURNED_ON_KEY, true)),
                WebAPILocator.getAnalyticsWebAPI(),
                Lazy.of(WebEventsCollectorServiceFactory::getInstance),
                new PagesAndUrlMapsRequestMatcher(),
                new FilesRequestMatcher(),
                //       new RulesRedirectsRequestMatcher(),
                new VanitiesRequestMatcher());

    }

    @VisibleForTesting
    public AnalyticsTrackWebInterceptor(final WhiteBlackList whiteBlackList,
                                        final AtomicBoolean isTurnedOn,
                                        final AnalyticsWebAPI analyticsWebAPI,
                                        final RequestMatcher... requestMatchers) {

        this(whiteBlackList, isTurnedOn, analyticsWebAPI, Lazy.of(WebEventsCollectorServiceFactory::getInstance),
                requestMatchers);
    }

    public AnalyticsTrackWebInterceptor(final WhiteBlackList whiteBlackList,
                                        final AtomicBoolean isTurnedOn,
                                        final AnalyticsWebAPI analyticsWebAPI,
                                        final Lazy<WebEventsCollectorServiceFactory> webEventsCollectorServiceFactory,
                                        final RequestMatcher... requestMatchers) {

        this.whiteBlackList = whiteBlackList;
        this.isTurnedOn = isTurnedOn;
        this.analyticsWebAPI = analyticsWebAPI;
        this.webEventsCollectorServiceFactory = webEventsCollectorServiceFactory;
    }


    /**
     * Add a request matchers
     * @param requestMatchers
     */
    public static void addRequestMatcher(final RequestMatcher... requestMatchers) {
        for (final RequestMatcher matcher : requestMatchers) {
            requestMatchersMap.put(matcher.getId(), matcher);
        }
    }

    /**
     * Remove a request matcher by id
     * @param requestMatcherId
     */
    public static void removeRequestMatcher(final String requestMatcherId) {

        requestMatchersMap.remove(requestMatcherId);
    }


    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

        try {
            if (isAllowed(request)) {

                if (isAutoInjectAndFeatureFlagIsOn(request)) {
                    injectCALib(request, response);
                    return Result.NEXT;
                }

                final Optional<RequestMatcher> matcherOpt = this.anyMatcher(request, response, RequestMatcher::runBeforeRequest);
                if (matcherOpt.isPresent()) {

                    addRequestId(request);
                    Logger.debug(this, () -> "intercept, Matched: " + matcherOpt.get().getId() + " request: " + request.getRequestURI());
                    fireNext(request, response, matcherOpt.get());
                }
            }
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        }

        return Result.NEXT;
    }

    /**
     * Adds the appropriate HTTP Headers to load the {@code ca.min.js} file when rendering
     * traditional HTML Pages in dotCMS.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     */
    private void injectCALib(HttpServletRequest request, HttpServletResponse response) {
        Logger.debug(this, () -> "Interceptor matched " + AUTO_INJECT_LIB_WEB_PATH + " file request: "
                + request.getRequestURI());
        response.addHeader(CONTENT_TYPE, "application/javascript; charset=utf-8");
        response.addHeader("access-control-allow-credentials", "true");
        response.addHeader("access-control-allow-headers",
                "Content-Type, Content-Length, Accept-Encoding, X-CSRF-Token, Authorization, Host");
        response.addHeader("access-control-allow-methods", "POST, GET, OPTIONS, PUT, DELETE, UPDATE, PATCH");
        response.addHeader("access-control-allow-origin", "*");
        response.addHeader("access-control-max-age", "86400");
    }

    private boolean isAutoInjectAndFeatureFlagIsOn(final HttpServletRequest request) {

        return request.getRequestURI().contains(AUTO_INJECT_LIB_WEB_PATH) && this.analyticsWebAPI.isAutoJsInjectionFlagOn();
    }

    /**
     * If the feature flag under {@link #ANALYTICS_TURNED_ON_KEY} is on
     * and there is any configuration for the analytics app
     * and the white black list allowed the current request
     * @param request
     * @return
     */
    private boolean isAllowed(final HttpServletRequest request) {

        return  isTurnedOn.get() &&
                this.analyticsWebAPI.anyAnalyticsConfig(request) &&
                whiteBlackList.isAllowed(request.getRequestURI());
    }


    private void addRequestId(final HttpServletRequest request) {
        if (null == request.getAttribute("requestId")) {
            request.setAttribute("requestId", UUIDUtil.uuid());
        }
    }

    @Override
    public boolean afterIntercept(final HttpServletRequest request, final HttpServletResponse response) {

        try {
            if (isAllowed(request)) {
                final Optional<RequestMatcher> matcherOpt = this.anyMatcher(request, response, RequestMatcher::runAfterRequest);
                if (matcherOpt.isPresent()) {

                    addRequestId(request);
                    Logger.debug(this, () -> "afterIntercept, Matched: " + matcherOpt.get().getId() + " request: " + request.getRequestURI());
                    fireNext(request, response, matcherOpt.get());
                }
            }
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        }

        return true;
    }

    private Optional<RequestMatcher> anyMatcher(final HttpServletRequest request, final HttpServletResponse response, Predicate<? super RequestMatcher> filterRequest) {

        return requestMatchersMap.values().stream()
                .filter(filterRequest)
                .filter(matcher -> matcher.match(request, response))
                .findFirst();
    }

    /**
     * Since the Fire the next step on the Analytics pipeline
     * @param request
     * @param response
     * @param requestMatcher
     */
    protected void fireNext(final HttpServletRequest request, final HttpServletResponse response,
                          final RequestMatcher requestMatcher) {

        Logger.debug(this, ()-> "fireNext, uri: " + request.getRequestURI() +
                " requestMatcher: " + requestMatcher.getId());
        webEventsCollectorServiceFactory.get().getWebEventsCollectorService().fireCollectors(request, response, requestMatcher);
    }


    @Override
    public void notify(final SystemTableUpdatedKeyEvent event) {
        if (event.getKey().contains(ANALYTICS_TURNED_ON_KEY)) {
            isTurnedOn.set(Config.getBooleanProperty(ANALYTICS_TURNED_ON_KEY, true));
        }
    }
}
