package com.dotmarketing.portlets.rules.conditionlet;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.CONTAINS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.ENDS_WITH;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.REGEX;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.STARTS_WITH;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.display.TextInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

/**
 * This conditionlet will allow CMS users to check whether a user has already
 * visited the current URL or not. The information on the visited pages will be
 * available until the user's session ends. The comparison of URLs is
 * case-insensitive, except for the regular expression comparison. This
 * {@link Conditionlet} provides a drop-down menu with the available comparison
 * mechanisms, and a text field to enter the value to compare. The user session
 * has a {@link Map} object holding the URLs that the user has visited per site.
 *
 * @author Jose Castro
 * @version 1.0
 * @since 04-23-2015
 *
 */
public class VisitedUrlConditionlet extends Conditionlet<VisitedUrlConditionlet.Instance> {

    private static final long serialVersionUID = 1L;

    public static final String RULES_CONDITIONLET_VISITEDURLS = "RULES_CONDITIONLET_VISITEDURLS";
    public static final String PATTERN_URL_INPUT_KEY = "has-visited-url";

    public VisitedUrlConditionlet() {
        super("api.ruleengine.system.conditionlet.HasVisitedUrl", new ComparisonParameterDefinition(2, IS, IS_NOT,
                STARTS_WITH, ENDS_WITH, CONTAINS, REGEX), patternUrl);
    }

    private static final ParameterDefinition<TextType> patternUrl = new ParameterDefinition<>(3, PATTERN_URL_INPUT_KEY,
            new TextInput<>(new TextType()));

    /**
     * Instance is guaranteed to be valid.
     */
    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        final String hostId = getHostId(request);
        if (!UtilMethods.isSet(hostId)) {
            return false;
        }

        HttpSession session = request.getSession(true);

        // Get visited urls from session variable
        Map<String, Set<String>> visitedUrls = (Map<String, Set<String>>) session
                .getAttribute(RULES_CONDITIONLET_VISITEDURLS);
        if (visitedUrls == null) {
            visitedUrls = new HashMap<String, Set<String>>();
        }

        // Get visited urls by host id from session variable
        Set<String> visitedUrlsByHost = visitedUrls.get(hostId);
        if (visitedUrlsByHost == null) {
            visitedUrlsByHost = new LinkedHashSet<String>();
        }

        // Find match with visited urls
        boolean match = hasMatch(visitedUrlsByHost, instance);

        // Add new url to session is not exist
        final String uri = getUri(request);
        if (StringUtils.isNotEmpty(uri) && !visitedUrlsByHost.contains(uri)) {
            visitedUrlsByHost.add(uri);
            visitedUrls.put(hostId, visitedUrlsByHost);
            session.setAttribute(RULES_CONDITIONLET_VISITEDURLS, visitedUrls);
        }

        return match;
    }

    /**
     * Verifies if condition matches with pattern, there are two different use
     * cases:
     * <ul>
     * <li>when comparison is different than IS_NOT. We need to find the first
     * match and return true otherwise false.</li>
     * <li>when comparison is equals to IS_NOT. We need to review all the
     * visited urls if all match return true otherwise false.</li>
     * </ul>
     * 
     * 
     * @param visitedUrlsByHost
     * @param instance
     * @return true is there is a match otherwise false
     */
    private boolean hasMatch(Set<String> visitedUrlsByHost, Instance instance) {
        final boolean comparisonIS_NOT = instance.comparisonValue.equalsIgnoreCase(IS_NOT.getId());

        // Variable must starts with true when IS_NOT comparison
        boolean match = comparisonIS_NOT;

        for (String url : visitedUrlsByHost) {
            if (comparisonIS_NOT) {
                match &= instance.comparison.perform(url, instance.patternUrl);
            } else {
                match |= instance.comparison.perform(url, instance.patternUrl);
            }

            if (!comparisonIS_NOT && match) {
                // We don't need to check all the visited urls when comparison
                // is different than IS_NOT
                break;
            }
        }

        return match;
    }

    /**
     * Returns the uri based on the {@code HttpServletRequest} object.
     * 
     * @param request
     *            - The {@code HttpServletRequest} object.
     * @return The URI of the request, or {@code null} if an error occurred..
     */
    private String getUri(HttpServletRequest request) {
        String uri = null;

        try {
            uri = HttpRequestDataUtil.getUri(request);
        } catch (UnsupportedEncodingException e) {
            Logger.error(this, "Could not retrieved a valid URI from request: " + request.getRequestURL());
        }

        return uri;
    }

    /**
     * Returns the ID of the site (host) based on the {@code HttpServletRequest}
     * object.
     *
     * @param request
     *            - The {@code HttpServletRequest} object.
     * @return The ID of the site, or {@code null} if an error occurred when
     *         retrieving the site information.
     */
    private String getHostId(HttpServletRequest request) {
        Host host = null;
        try {
            host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
        } catch (PortalException | SystemException | DotDataException | DotSecurityException e) {
            Logger.error(this, "Could not retrieve current host information.");
        }

        return host != null ? host.getIdentifier() : null;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {

        private final String patternUrl;
        private final Comparison<String> comparison;
        private final String comparisonValue;

        private Instance(VisitedUrlConditionlet definition, Map<String, ParameterModel> parameters) {
            this.patternUrl = parameters.get(PATTERN_URL_INPUT_KEY).getValue();
            this.comparisonValue = parameters.get(COMPARISON_KEY).getValue();

            try {
                // noinspection unchecked
                this.comparison = ((ComparisonParameterDefinition) definition.getParameterDefinitions().get(
                        COMPARISON_KEY)).comparisonFrom(comparisonValue);
            } catch (ComparisonNotPresentException e) {
                throw new ComparisonNotSupportedException(
                        "The comparison '%s' is not supported on Condition type '%s'", comparisonValue,
                        definition.getId());
            }
        }
    }
}
