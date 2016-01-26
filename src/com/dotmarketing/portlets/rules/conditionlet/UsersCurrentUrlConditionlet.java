package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotcms.repackage.com.google.common.collect.Sets;
import com.dotcms.rest.exception.InvalidConditionParameterException;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.conditionlet.VisitedUrlConditionlet.Instance;
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
import com.dotmarketing.util.UtilMethods;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.CONTAINS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.ENDS_WITH;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
//import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.REGEX;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.STARTS_WITH;

/**
 * This conditionlet will allow CMS users to check the current URL in a request.
 * The comparison of URLs is case-insensitive, except for the regular expression
 * comparison. This {@link Conditionlet} provides a drop-down menu with the
 * available comparison mechanisms, and a text field to enter the value to
 * compare.
 *
 */
public class UsersCurrentUrlConditionlet extends Conditionlet<UsersCurrentUrlConditionlet.Instance> {

	private static final long serialVersionUID = 1L;

	public static final String PATTERN_URL_INPUT_KEY = "current-url";

	public UsersCurrentUrlConditionlet() {
		super("api.ruleengine.system.conditionlet.UsersCurrentUrl", new ComparisonParameterDefinition(2, IS, IS_NOT,
                //STARTS_WITH, ENDS_WITH, CONTAINS, REGEX), patternUrl);
				STARTS_WITH, ENDS_WITH, CONTAINS), patternUrl);
	}

	private static final ParameterDefinition<TextType> patternUrl = new ParameterDefinition<>(3, PATTERN_URL_INPUT_KEY,
            new LocalUrlTextInput(new TextType()));

	@Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        String requestUri = null;
		try {
			requestUri = HttpRequestDataUtil.getUri(request);
		} catch (UnsupportedEncodingException e) {
			Logger.error(this, "Could not retrieved a valid URI from request: "
					+ request.getRequestURL());
		}
		if (!UtilMethods.isSet(requestUri)) {
			return false;
		}
		return instance.comparison.perform(requestUri, instance.patternUrl);
	}

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
    	return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {
    	private final String patternUrl;
        private final Comparison<String> comparison;
        private final String comparisonValue;

        private Instance(UsersCurrentUrlConditionlet definition, Map<String, ParameterModel> parameters) {
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

    private static class LocalUrlTextInput extends TextInput<TextType>{

		public LocalUrlTextInput(TextType dataType) {
			super(dataType);
		}

		/**
	     * Validates the parameter context for the conditionlet. Each input will implement this validation if required.
	     * @param value parameter value
	     * @throws InvalidConditionParameterException
	     */
	    public void checkValid(String value)  throws InvalidConditionParameterException{

	    	String url = value.indexOf("?")>0?value.substring(0,value.indexOf("?")):value;
	    	if(!url.startsWith("/"))
	    		throw new InvalidConditionParameterException("URL parameter '%s' is malformed, should start with '/'",value);
	    	if(url.contains(" "))
	    		throw new InvalidConditionParameterException("URL parameter '%s' should not have white spaces",value);
	    	return;
	    }
    }
}
