package com.dotmarketing.portlets.rules.conditionlet;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.CONTAINS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.ENDS_WITH;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.REGEX;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.STARTS_WITH;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.comparison.RegexComparison;
import com.dotmarketing.portlets.rules.parameter.display.TextInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;

/**
 * This conditionlet will allow CMS users to check the current URL in a request.
 * The comparison of URLs is case-insensitive, except for the regular expression
 * comparison. This {@link Conditionlet} provides a drop-down menu with the
 * available comparison mechanisms, and a text field to enter the value to
 * compare. The URL input value is required.
 *
 * As part of dotCMS functionality the 'CMS_INDEX_PAGE' property is used to imply the 'index'
 * value of a directory, so if a directory such as '/contact-us/' is used on the
 * conditionlet remember to by check if the directory has an index page set, if
 * it does the conditionlet should test against '/contact-us/index' to evaluate
 * the URL correctly
 *
 */
public class VisitorsCurrentUrlConditionlet extends Conditionlet<VisitorsCurrentUrlConditionlet.Instance> {

	private static final long serialVersionUID = 1L;

	public static final String CURRENT_URL_CONDITIONLET_MATCHER = "CURRENT_URL_CONDITIONLET_MATCHER";

	public static final String PATTERN_URL_INPUT_KEY = "current-url";

	public VisitorsCurrentUrlConditionlet() {
		super("api.ruleengine.system.conditionlet.VisitorsCurrentUrl", new ComparisonParameterDefinition(2, IS, IS_NOT,
                STARTS_WITH, ENDS_WITH, CONTAINS, REGEX), patternUrl);
	}

	private static final ParameterDefinition<TextType> patternUrl = new ParameterDefinition<>(3, PATTERN_URL_INPUT_KEY,
            new TextInput<>(new TextType().required()));

	@Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        String[] requestUri = getURIsFromRequest(request);

		String index = CMSFilter.CMS_INDEX_PAGE;
		String pattern = processUrl(instance.patternUrl,index, instance);

		return evaluate(request, instance, requestUri, pattern);
	}

    public boolean evaluate(HttpServletRequest request, Instance instance, String[] requestUris, String pattern) {

        for (String requestUri : requestUris) {
            if (instance.comparison.perform(requestUri, pattern)) {
                if (instance.comparison.getId().equals(REGEX.getId())) {
                    Pattern regex = RegexComparison.patternsCache.get(pattern, k -> Pattern.compile(k));
                    request.setAttribute(CURRENT_URL_CONDITIONLET_MATCHER, regex);
                }
                return true;
            }
        }
        return false;
    }

	
    /**
     * Get the requestURI from the urlmap and/or vanity url. If the request has been forwarded, both the
     * origial URI (urlmap) and the new uri (detail page) will be evaluated.
     * 
     * @param request
     * @return
     */
    private String[] getURIsFromRequest(HttpServletRequest request) {
        final String cmsURI = CMSUrlUtil.getInstance().getURIFromRequest(request);
        final String forwardedFor = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
        return (forwardedFor == null)
            ? new String[] {
                    cmsURI}
            : new String[] {
                    forwardedFor,
                    cmsURI};

    }
	
	
	
	
	
	/**
	 * Process the baseUrl to comply with:
	 * <ul><li>Does not include query params</li>
	 * <li>If a person enters a string that .endsWith("/") , e.g. is a folder, we need to
	 * evaluate against the path + the Config variable for CMS_INDEX_PAGE, whatever that is,
	 * e.g. /news-events/news/ checks against /news-events/news/index, this happens on IS,
	 * IS_NOT and ENDS_WITH to ensure that the user can use STARTS_WITH or REGEXP without
	 * affecting top tier folders structure so STARTS_WITH '/folder/' means everything under
	 * the folder not only '/folder/index'. </li></ul>
	 */
	public String processUrl(String baseUrl, String index, Instance instance){
		String processedUrl = baseUrl;
		if(processedUrl.indexOf("?") > 0)
			processedUrl = processedUrl.substring(0,processedUrl.indexOf("?"));
		if(instance.comparison.getId().equals(IS.getId())
				|| instance.comparison.getId().equals(IS_NOT.getId())
				|| instance.comparison.getId().equals(ENDS_WITH.getId())){
			if(processedUrl.endsWith("/"))
				processedUrl = processedUrl + index;
		}
		return processedUrl;
	}

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
    	return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {
    	private final String patternUrl;
        private final Comparison<String> comparison;
        private final String comparisonValue;

        private Instance(VisitorsCurrentUrlConditionlet definition, Map<String, ParameterModel> parameters) {
        	checkState(parameters != null && parameters.size() == 2, "Current URL Condition requires parameters %s and %s.", COMPARISON_KEY, PATTERN_URL_INPUT_KEY);
            assert parameters != null;
            this.patternUrl = parameters.get(PATTERN_URL_INPUT_KEY).getValue();
            this.comparisonValue = parameters.get(COMPARISON_KEY).getValue();

            try {
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
