package com.dotcms.analytics.track;

import com.dotcms.util.ConversionUtils;
import com.dotmarketing.filters.Constants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

/**
 * Matcher for vanity urls and rules redirect
 * @author jsanca
 */
public class VanitiesRequestMatcher implements RequestMatcher {

    @Override
    public boolean runAfterRequest() {
        return true;
    }

    @Override
    public boolean match(final HttpServletRequest request, final HttpServletResponse response) {

        final Object vanityHasRun = request.getAttribute(Constants.VANITY_URL_HAS_RUN);

        return  Objects.nonNull(vanityHasRun) && ConversionUtils.toBooleanFromDb(vanityHasRun);
    }
}
