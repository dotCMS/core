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
public class UsersVisitedUrlConditionlet extends Conditionlet<UsersVisitedUrlConditionlet.Instance> {

    private static final long serialVersionUID = 1L;

    public static final String RULES_CONDITIONLET_VISITEDURLS = "RULES_CONDITIONLET_VISITEDURLS";
    public static final String PATTERN_URL_INPUT_KEY = "has-visited-url";

    // private Map<String, ConditionletInput> inputValues = null;

    public UsersVisitedUrlConditionlet() {
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

        Map<String, Set<String>> visitedUrls = (Map<String, Set<String>>) request.getSession(true).getAttribute(
                RULES_CONDITIONLET_VISITEDURLS);
        Set<String> visitedUrlsByHost = visitedUrls == null ? new LinkedHashSet<String>() : visitedUrls.get(hostId);

        // Find if there visited urls match with the pattern
        boolean visitedMatch = false;
        for (String url : visitedUrlsByHost) {
            if (instance.comparison.perform(url, instance.patternUrl)) {
                visitedMatch = true;
                break;
            }
        }

        // Add new url to session is not exist
        updateVisitedUrlsSessionVariable(request, hostId);

        return visitedMatch;
    }

    /**
     * 
     */
    private void updateVisitedUrlsSessionVariable(HttpServletRequest request, final String hostId) {
        Map<String, Set<String>> visitedUrls = (Map<String, Set<String>>) request.getSession(true).getAttribute(
                RULES_CONDITIONLET_VISITEDURLS);
        Set<String> visitedUrlsByHost = visitedUrls == null ? new LinkedHashSet<String>() : visitedUrls.get(hostId);
        if (visitedUrls == null) {
            visitedUrls = new HashMap<String, Set<String>>();
        }

        try {
            final String uri = HttpRequestDataUtil.getUri(request);

            if (UtilMethods.isSet(uri) && !visitedUrlsByHost.contains(uri)) {
                visitedUrlsByHost.add(uri);
                visitedUrls.put(hostId, visitedUrlsByHost);
                request.getSession(true).setAttribute(RULES_CONDITIONLET_VISITEDURLS, visitedUrls);
            }
        } catch (UnsupportedEncodingException e) {
            Logger.error(this, "Could not retrieved a valid URI from request: " + request.getRequestURL());
        }
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
        try {
            Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
            if (host != null) {
                return host.getIdentifier();
            }
        } catch (PortalException | SystemException | DotDataException | DotSecurityException e) {
            Logger.error(this, "Could not retrieve current host information.");
        }
        return null;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {

        private final String patternUrl;
        private final Comparison<String> comparison;

        private Instance(UsersVisitedUrlConditionlet definition, Map<String, ParameterModel> parameters) {
            this.patternUrl = parameters.get(PATTERN_URL_INPUT_KEY).getValue();
            String comparisonValue = parameters.get(COMPARISON_KEY).getValue();
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
