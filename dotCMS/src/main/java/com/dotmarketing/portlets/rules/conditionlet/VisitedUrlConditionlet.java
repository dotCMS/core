package com.dotmarketing.portlets.rules.conditionlet;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.CONTAINS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.ENDS_WITH;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.REGEX;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.STARTS_WITH;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.filters.Constants;
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
    	return evaluate(request, response, CMSFilter.CMS_INDEX_PAGE, instance);
    }

    public boolean evaluate(HttpServletRequest request, HttpServletResponse response,String index, Instance instance) {
        final String hostId = getHostId(request);
        
        boolean returnValue=IS_NOT.getId().equalsIgnoreCase(instance.comparisonValue);
        
        
        if (!UtilMethods.isSet(hostId)) {
            return returnValue;
        }

        HttpSession session = request.getSession(false);
        if(session==null || session.isNew()) {
          return returnValue;
        }
        Clickstream clickstream = (Clickstream) session.getAttribute("clickstream");
        if (clickstream == null || clickstream.getClickstreamRequests()==null) {
          return returnValue;
       }

        final List<String> urls = new ArrayList<>();
        urls.add(request.getRequestURI());
        clickstream.getClickstreamRequests().forEach(c->urls.add(c.getRequestURI()));
        
        // Find match with visited urls
        boolean match = hasMatch(urls, index, instance);


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
    private boolean hasMatch(List<String> urls, String index, Instance instance) {
        final boolean comparisonIS_NOT = instance.comparisonValue.equalsIgnoreCase(IS_NOT.getId());

        // Variable must starts with true when IS_NOT comparison
        boolean match = comparisonIS_NOT;

        String pattern = processUrl(instance.patternUrl, index, instance.comparison);

        
        
        
        
        for (String url : urls) {
            if (comparisonIS_NOT) {
                match &= instance.comparison.perform(url, pattern);
            } else {
                match |= instance.comparison.perform(url, pattern);
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
	 * Process the baseUrl to comply with:
	 * <ul><li>Does not include query params</li>
	 * <li>If a person enters a string that .endsWith(“/”) , e.g. is a folder, we need to
	 * evaluate against the path + the Config variable for CMS_INDEX_PAGE, whatever that is,
	 * e.g. /news-events/news/ checks against /news-events/news/index, this happens on IS,
	 * IS_NOT and ENDS_WITH to ensure that the user can use STARTS_WITH or REGEXP without
	 * affecting top tier folders structure so STARTS_WITH '/folder/' means everything under
	 * the folder not only '/folder/index'. </li></ul>
	 * @param baseUrl
	 * @return
	 */
	private String processUrl(String baseUrl, String index, Comparison comparison){
		String processedUrl = baseUrl;
		if(processedUrl.indexOf("?") > 0)
			processedUrl = processedUrl.substring(0,processedUrl.indexOf("?"));
		if(comparison.getId().equals(IS.getId())
				|| comparison.getId().equals(IS_NOT.getId())
				|| comparison.getId().equals(ENDS_WITH.getId())){
			if(processedUrl.endsWith("/"))
				processedUrl = processedUrl + index;
		}
		return processedUrl;
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
            Object rewriteOpt = request.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE);
			if(rewriteOpt != null)
				uri = (String) rewriteOpt;
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
