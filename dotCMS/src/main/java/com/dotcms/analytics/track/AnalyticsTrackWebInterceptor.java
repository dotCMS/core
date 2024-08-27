package com.dotcms.analytics.track;

import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.jitsu.EventLogSubmitter;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;

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

    public AnalyticsTrackWebInterceptor() {

        submitter = new EventLogSubmitter();
        addRequestMatcher(new PagesUrlMapsAndFileRequestMatcher(), new RuleVanityRequestMatcher());
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

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

        final Optional<RequestMatcher> matcherOpt = this.anyMatcher(request, response, RequestMatcher::runBeforeRequest);
        if (matcherOpt.isPresent()) {

            Logger.debug(this, ()-> "intercept, Matched: " + matcherOpt.get().getId() + " request: " + request.getRequestURI());
            //fireNextStep(request, response);
        }

        return Result.NEXT;
    }

    @Override
    public boolean afterIntercept(final HttpServletRequest request, final HttpServletResponse response) {

        //var vanityUrlHasRun = request.getAttribute(Constants.VANITY_URL_HAS_RUN);
        //if (Objects.nonNull(vanityUrlHasRun) && ConversionUtils.toBooleanFromDb(vanityUrlHasRun)) {

        final Optional<RequestMatcher> matcherOpt = this.anyMatcher(request,  response, RequestMatcher::runAfterRequest);
        if (matcherOpt.isPresent()) {

            Logger.debug(this, ()-> "afterIntercept, Matched: " + matcherOpt.get().getId() + " request: " + request.getRequestURI());
            //fireNextStep(request, response);
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
