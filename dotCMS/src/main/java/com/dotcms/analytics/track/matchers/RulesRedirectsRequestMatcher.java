package com.dotcms.analytics.track.matchers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

/**
 * Matcher for vanity urls and rules redirect
 * @author jsanca
 */
public class RulesRedirectsRequestMatcher implements RequestMatcher {

    public static final String RULES_MATCHER_ID = "rules";

    @Override
    public boolean runAfterRequest() {
        return true;
    }

    @Override
    public boolean match(final HttpServletRequest request, final HttpServletResponse response) {

        final String ruleRedirect = response.getHeader("X-DOT-SendRedirectRuleAction");
        return Objects.nonNull(ruleRedirect) && "true".equals(ruleRedirect);
    }

    @Override
    public String getId() {
        return RULES_MATCHER_ID;
    }
}
