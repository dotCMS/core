package com.dotcms.analytics.track;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

/**
 * Matcher for vanity urls and rules redirect
 * @author jsanca
 */
public class RulesRedirectsRequestMatcher implements RequestMatcher {

    @Override
    public boolean runAfterRequest() {
        return true;
    }

    @Override
    public boolean match(final HttpServletRequest request, final HttpServletResponse response) {

        final String ruleRedirect = response.getHeader("X-DOT-SendRedirectRuleAction");
        return Objects.nonNull(ruleRedirect) && "true".equals(ruleRedirect);
    }
}
