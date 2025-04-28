package com.dotcms.analytics.track.matchers;

import com.dotcms.analytics.track.AnalyticsTrackWebInterceptor;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HttpResponseMatcher implements RequestMatcher {

    public static String HTTP_RESPONSE_MATCHER_ID = "http-response-matcher";

    @Override
    public boolean runBeforeRequest() {
        return false;
    }

    @Override
    public boolean runAfterRequest() {
        return true;
    }

    @Override
    public boolean match(final HttpServletRequest request, final HttpServletResponse response) {
        final User loggedInUser = APILocator.getLoginServiceAPI().getLoggedInUser();

        return loggedInUser.isFrontendUser() ||
             AnalyticsTrackWebInterceptor.anyMatcherBeforeRequest(request, response);
    }

    @Override
    public String getId() {
        return HTTP_RESPONSE_MATCHER_ID;
    }

}
