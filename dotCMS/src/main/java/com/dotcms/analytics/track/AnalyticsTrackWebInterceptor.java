package com.dotcms.analytics.track;

import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.jitsu.EventLogSubmitter;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.WhiteBlackList;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;

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

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

        if (whiteBlackList.isAllowed(request.getRequestURI())) {
            final Optional<RequestMatcher> matcherOpt = this.anyMatcher(request, response, RequestMatcher::runBeforeRequest);
            if (matcherOpt.isPresent()) {

                Logger.debug(this, () -> "intercept, Matched: " + matcherOpt.get().getId() + " request: " + request.getRequestURI());
                //fireNextStep(request, response);
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
                //fireNextStep(request, response);
            }
        }

        return true;
    }

    private Optional<RequestMatcher> anyMatcher(final HttpServletRequest request, final HttpServletResponse response, Predicate<? super RequestMatcher> afterRequest) {

        return requestMatchersMap.values().stream()
                .filter(afterRequest)
                .filter(matcher -> matcher.match(request, response))
                .findFirst();
    }

}
