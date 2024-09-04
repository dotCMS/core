package com.dotcms.analytics.track;

import com.dotcms.analytics.track.collectors.CharacterCollectorContextMap;
import com.dotcms.analytics.track.collectors.Collector;
import com.dotcms.analytics.track.collectors.CollectorContextMap;
import com.dotcms.analytics.track.collectors.CollectorPayloadBean;
import com.dotcms.analytics.track.collectors.ConcurrentCollectorPayloadBean;
import com.dotcms.analytics.track.collectors.RequestCharacterCollectorContextMap;
import com.dotcms.analytics.track.matchers.FilesRequestMatcher;
import com.dotcms.analytics.track.matchers.PagesAndUrlMapsRequestMatcher;
import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotcms.analytics.track.matchers.RulesRedirectsRequestMatcher;
import com.dotcms.analytics.track.matchers.VanitiesRequestMatcher;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.http.CircuitBreakerUrlBuilder;
import com.dotcms.jitsu.EventLogRunnable;
import com.dotcms.jitsu.EventLogSubmitter;
import com.dotcms.jitsu.EventsPayload;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.WhiteBlackList;
import com.dotcms.visitor.filter.characteristics.Character;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;
import org.apache.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Web Interceptor to track analytics
 * @author jsanca
 */
public class AnalyticsTrackWebInterceptor  implements WebInterceptor {

    private final static Map<String, RequestMatcher> requestMatchersMap = new ConcurrentHashMap<>();
    private final static Map<String, Collector> syncCollectors  = new ConcurrentHashMap<>();
    private final static Map<String, Collector> asyncCollectors = new ConcurrentHashMap<>();

    private final EventLogSubmitter submitter;

    /// private static final String[] DEFAULT_BLACKLISTED_PROPS = new String[]{"^/api/*"};
    private static final String[] DEFAULT_BLACKLISTED_PROPS = new String[]{StringPool.BLANK};
    private final WhiteBlackList whiteBlackList = new WhiteBlackList.Builder()
            .addWhitePatterns(Config.getStringArrayProperty("ANALYTICS_WHITELISTED_KEYS",
                    new String[]{StringPool.BLANK})) // allows everything
            .addBlackPatterns(CollectionsUtils.concat(Config.getStringArrayProperty(  // except this
                    "ANALYTICS_BLACKLISTED_KEYS", new String[]{}), DEFAULT_BLACKLISTED_PROPS)).build();

    public AnalyticsTrackWebInterceptor() {

        submitter = new EventLogSubmitter();
        addRequestMatcher(
                new PagesAndUrlMapsRequestMatcher(),
                new FilesRequestMatcher(),
                new RulesRedirectsRequestMatcher(),
                new VanitiesRequestMatcher());
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

    /**
     * Add a collector
     * @param collectors
     */
    public static void addCollector(final Collector... collectors) {
        for (final Collector collector : collectors) {
            if (collector.isAsync()) {

                asyncCollectors.put(collector.getId(), collector);
            } else {
                syncCollectors.put(collector.getId(), collector);
            }
        }
    }

    /**
     * Remove a collector by id
     * @param collectorId
     */
    public static void removeCollector(final String collectorId) {

        if (syncCollectors.containsKey(collectorId)) {
            syncCollectors.remove(collectorId);
        }

        if (asyncCollectors.containsKey(collectorId)) {
            asyncCollectors.remove(collectorId);
        }
    }

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

        if (whiteBlackList.isAllowed(request.getRequestURI())) {
            final Optional<RequestMatcher> matcherOpt = this.anyMatcher(request, response, RequestMatcher::runBeforeRequest);
            if (matcherOpt.isPresent()) {

                Logger.debug(this, () -> "intercept, Matched: " + matcherOpt.get().getId() + " request: " + request.getRequestURI());
                fireNext(request, response, matcherOpt.get());
            }
        }

        return Result.NEXT;
    }

    @Override
    public boolean afterIntercept(final HttpServletRequest request, final HttpServletResponse response) {

        if (whiteBlackList.isAllowed(request.getRequestURI())) {
            final Optional<RequestMatcher> matcherOpt = this.anyMatcher(request, response, RequestMatcher::runAfterRequest);
            if (matcherOpt.isPresent()) {

                Logger.debug(this, () -> "afterIntercept, Matched: " + matcherOpt.get().getId() + " request: " + request.getRequestURI());
                fireNext(request, response, matcherOpt.get());
            }
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

        if (!asyncCollectors.isEmpty() || !syncCollectors.isEmpty()) {

            final CollectorPayloadBean collectorPayloadBean = new ConcurrentCollectorPayloadBean();
            this.runCollectors(request, response, requestMatcher, collectorPayloadBean);
        }
    }

    private void runCollectors(final HttpServletRequest request,
                               final HttpServletResponse response,
                               final RequestMatcher requestMatcher,
                               final CollectorPayloadBean collectorPayloadBean) {

        final Character character = WebAPILocator.getCharacterWebAPI().getOrCreateCharacter(request, response);
        final Host site = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        if (!syncCollectors.isEmpty()) {

            Logger.debug(this, ()-> "Running sync collectors");
            final CollectorContextMap syncCollectorContextMap = new RequestCharacterCollectorContextMap(request, character, requestMatcher);
            // we collect info which is sync and includes the request.
            syncCollectors.values().stream().filter(collector -> collector.test(syncCollectorContextMap)).forEach(collector -> collector.collect(syncCollectorContextMap, collectorPayloadBean));
        }

        // if there is anything to run async
        final CollectorContextMap collectorContextMap = new CharacterCollectorContextMap(character, requestMatcher);
        this.submitter.logEvent(
                new EventLogRunnable(site, ()-> {
                    Logger.debug(this, ()-> "Running async collectors");
                    asyncCollectors.values().stream()
                            .filter(collector -> collector.test(collectorContextMap))
                            .forEach(collector -> { collector.collect(collectorContextMap, collectorPayloadBean); });
                    return collectorPayloadBean.toMap();
                }));

    }


}
