package com.dotcms.analytics.track.matchers;

import com.dotcms.visitor.filter.characteristics.Character;
import com.dotcms.visitor.filter.characteristics.CharacterWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.filters.CMSFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

/**
 * Matcher to be executed all the time before response
 * @author jsanca
 */
public class HttpResponseMatcher implements RequestMatcher {

    public static final String HTTP_RESPONSE_MATCHER = "http-response-matcher";

    @Override
    public boolean runAfterRequest() {
        return true;
    }

    @Override
    public boolean match(final HttpServletRequest request, final HttpServletResponse response) {

        return true;
    }

    @Override
    public String getId() {
        return HTTP_RESPONSE_MATCHER;
    }
}
