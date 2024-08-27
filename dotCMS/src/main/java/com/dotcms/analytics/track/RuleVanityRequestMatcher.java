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
public class RuleVanityRequestMatcher implements RequestMatcher {

    @Override
    public boolean runAfterRequest() {
        return true;
    }

    @Override
    public boolean match(final HttpServletRequest request, final HttpServletResponse response) {

        final String ruleRedirect = response.getHeader("X-DOT-SendRedirectRuleAction");
        final Object vanityHasRun = request.getAttribute(Constants.VANITY_URL_HAS_RUN);

        return (Objects.nonNull(ruleRedirect) && "true".equals(ruleRedirect))
                || (Objects.nonNull(vanityHasRun) && ConversionUtils.toBooleanFromDb(vanityHasRun));
    }
}
