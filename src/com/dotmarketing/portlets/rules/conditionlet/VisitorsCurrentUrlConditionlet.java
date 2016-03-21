package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.display.TextInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.util.Logger;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.CONTAINS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.ENDS_WITH;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.REGEX;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.STARTS_WITH;

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

	public static final String PATTERN_URL_INPUT_KEY = "current-url";

	public VisitorsCurrentUrlConditionlet() {
		super("api.ruleengine.system.conditionlet.VisitorsCurrentUrl", new ComparisonParameterDefinition(2, IS, IS_NOT,
                STARTS_WITH, ENDS_WITH, CONTAINS, REGEX), patternUrl);
	}

	private static final ParameterDefinition<TextType> patternUrl = new ParameterDefinition<>(3, PATTERN_URL_INPUT_KEY,
            new TextInput<>(new TextType().required()));

	@Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        String requestUri = null;

		try {
			requestUri = HttpRequestDataUtil.getUri(request);
			Object rewriteOpt = request.getAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE);
			if(rewriteOpt != null)
				requestUri = (String) rewriteOpt;
		} catch (UnsupportedEncodingException e) {
			Logger.error(this, "Could not retrieved a valid URI from request: "
					+ request.getRequestURL());
		}

		String index = CMSFilter.CMS_INDEX_PAGE;
		String pattern = processUrl(instance.patternUrl,index, instance);

		return evaluate(request, instance, requestUri, pattern);
	}

	public boolean evaluate(HttpServletRequest request, Instance instance, String requestUri, String pattern) {
		return instance.comparison.perform(requestUri, pattern);
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
