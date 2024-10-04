package com.dotcms.analytics.track;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.track.collectors.WebEventsCollectorServiceFactory;
import com.dotcms.analytics.track.matchers.FilesRequestMatcher;
import com.dotcms.analytics.track.matchers.PagesAndUrlMapsRequestMatcher;
import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotcms.analytics.track.matchers.VanitiesRequestMatcher;
import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.WhiteBlackList;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Web Interceptor to track analytics
 * @author jsanca
 */
public class AnalyticsTrackWebInterceptor  implements WebInterceptor, EventSubscriber<SystemTableUpdatedKeyEvent> {

    private static final  String[] DEFAULT_BLACKLISTED_PROPS = new String[]{StringPool.BLANK};
    private static final  String ANALYTICS_TURNED_ON_KEY = "FEATURE_FLAG_CONTENT_ANALYTICS";
    private static final  Map<String, RequestMatcher> requestMatchersMap = new ConcurrentHashMap<>();
    private final HostWebAPI hostWebAPI;
    private final AppsAPI appsAPI;
    private final Supplier<User> systemUserSupplier;

    private final WhiteBlackList whiteBlackList;
    private final AtomicBoolean isTurnedOn;

    public AnalyticsTrackWebInterceptor() {

        this(WebAPILocator.getHostWebAPI(), APILocator.getAppsAPI(),
                new WhiteBlackList.Builder()
                        .addWhitePatterns(Config.getStringArrayProperty("ANALYTICS_WHITELISTED_KEYS",
                                new String[]{StringPool.BLANK})) // allows everything
                        .addBlackPatterns(CollectionsUtils.concat(Config.getStringArrayProperty(  // except this
                                "ANALYTICS_BLACKLISTED_KEYS", new String[]{}), DEFAULT_BLACKLISTED_PROPS)).build(),
                new AtomicBoolean(Config.getBooleanProperty(ANALYTICS_TURNED_ON_KEY, true)),
                ()->APILocator.systemUser(),
                new PagesAndUrlMapsRequestMatcher(),
                new FilesRequestMatcher(),
                //       new RulesRedirectsRequestMatcher(),
                new VanitiesRequestMatcher());

    }

    public AnalyticsTrackWebInterceptor(final HostWebAPI hostWebAPI,
                        final AppsAPI appsAPI,
                        final WhiteBlackList whiteBlackList,
                        final AtomicBoolean isTurnedOn,
                        final Supplier<User> systemUser,
                        final RequestMatcher... requestMatchers) {

        this.hostWebAPI = hostWebAPI;
        this.appsAPI    = appsAPI;
        this.whiteBlackList = whiteBlackList;
        this.isTurnedOn = isTurnedOn;
        this.systemUserSupplier = systemUser;
        addRequestMatcher(requestMatchers);
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
     * If the feature flag under {@link #ANALYTICS_TURNED_ON_KEY} is on
     * and there is any configuration for the analytics app
     * and the white black list allowed the current request
     * @param request
     * @return
     */
    private boolean isAllowed(final HttpServletRequest request) {

        return  isTurnedOn.get() &&
                anyConfig(request) &&
                whiteBlackList.isAllowed(request.getRequestURI());
    }

    private boolean anyConfig(final HttpServletRequest request) {

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
        WebEventsCollectorServiceFactory.getInstance().getWebEventsCollectorService().fireCollectors(request, response, requestMatcher);
    }


    @Override
    public void notify(final SystemTableUpdatedKeyEvent event) {
        if (event.getKey().contains(ANALYTICS_TURNED_ON_KEY)) {
            isTurnedOn.set(Config.getBooleanProperty(ANALYTICS_TURNED_ON_KEY, true));
        }
    }
}
