package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.ValidationResult;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.util.Logger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.dotmarketing.portlets.rules.conditionlet.Comparison.IS;

/**
 * This conditionlet will allow CMS users to check the browser name a user
 * request is issued from. The information is obtained by reading the
 * {@code User-Agent} header in the {@link HttpServletRequest} object. The
 * comparison of browser names is case-insensitive, except for the regular
 * expression comparison. This {@link Conditionlet} provides a drop-down menu
 * with the available comparison mechanisms, and a single text field to enter
 * the value to compare.
 * <p>
 * The format of the {@code User-Agent} is not standardized (basically free
 * format), which makes it difficult to decipher it. This conditionlet uses a
 * Java API called <a
 * href="http://www.bitwalker.eu/software/user-agent-utils">User Agent Utils</a>
 * which parses HTTP requests in real time and gather information about the user
 * agent, detecting a high amount of browsers, browser types, operating systems,
 * device types, rendering engines, and Web applications.
 * </p>
 * <p>
 * The User Agent Utils API uses regular expressions to extract the browser's
 * name from the {@code User-Agent} header. Given that the format is not
 * standard, the name might also contain version numbers. For example, the API
 * can return values like "Chrome", "Safari 7", "Firefox 37",
 * "Internet Explorer 11", and so on. Therefore, if you need to validate against
 * a browser in general without considering versions, say "Firefox", you can
 * select the "Contains" comparison to just lookup the word "Firefox".
 * </p>
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 04-21-2015
 *
 */
public class UsersBrowserConditionlet extends Conditionlet<UsersBrowserConditionlet.Instance> {

	private static final long serialVersionUID = 1L;

	private static final String INPUT_ID = "browser";
	private static final String CONDITIONLET_NAME = "User's Browser";

	private Map<String, ConditionletInput> inputValues = null;

	public UsersBrowserConditionlet() {
		super(CONDITIONLET_NAME, ImmutableSet.of(IS,
                                                     Comparison.IS_NOT,
                                                     Comparison.STARTS_WITH,
                                                     Comparison.ENDS_WITH,
                                                     Comparison.CONTAINS,
                                                     Comparison.REGEX));
	}


	protected ValidationResult validate(Comparison comparison, ConditionletInputValue inputValue) {
		ValidationResult validationResult = new ValidationResult();
		String inputId = inputValue.getConditionletInputId();
			String selectedValue = inputValue.getValue();
			if (comparison == IS
					|| comparison == Comparison.IS_NOT
					|| comparison == Comparison.STARTS_WITH
					|| comparison == Comparison.ENDS_WITH
					|| comparison == Comparison.CONTAINS) {
					validationResult.setValid(true);
			} else if (comparison == Comparison.REGEX) {
				try {
					Pattern.compile(selectedValue);
					validationResult.setValid(true);
				} catch (PatternSyntaxException e) {
					Logger.debug(this, "Invalid RegEx " + selectedValue);
				}
			}
			if (!validationResult.isValid()) {
				validationResult.setErrorMessage("Invalid value for input '%s': '%s'", inputId, selectedValue);
			}
		return validationResult;
	}

	public Collection<ConditionletInput> getInputs(String comparisonId) {
		if (this.inputValues == null) {
			ConditionletInput inputField = new ConditionletInput();
			inputField.setId(INPUT_ID);
			inputField.setUserInputAllowed(true);
			inputField.setMultipleSelectionAllowed(false);
			inputField.setMinNum(1);
			this.inputValues = new LinkedHashMap<String, ConditionletInput>();
			this.inputValues.put(inputField.getId(), inputField);
		}
		return this.inputValues.values();
	}

	@Override
	public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
//		String userAgentInfo = request.getHeader("User-Agent");
//		UserAgent agent = UserAgent.parseUserAgentString(userAgentInfo);
//		String browserName = null;
//		if (agent != null && agent.getBrowser() != null) {
//			browserName = agent.getBrowser().getName();
//		}
//		if (!UtilMethods.isSet(browserName)) {
//			return false;
//		}
//		Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
//		String inputValue = values.get(0).getValue();
//		inputValues.add(new ConditionletInputValue(INPUT_ID, inputValue));
//		ValidationResults validationResults = validate(comparison, inputValues);
//		if (validationResults.hasErrors()) {
//			return false;
//		}
//		if (comparison != Comparison.REGEX) {
//			inputValue = inputValue.toLowerCase();
//			browserName = browserName.toLowerCase();
//		}
//        if(comparison == Comparison.IS) {
//            if(browserName.equals(inputValue)) {
//                return true;
//            }
//        } else if(comparison == Comparison.IS_NOT) {
//            if(!browserName.equals(inputValue)) {
//                return true;
//            }
//        } else if(comparison == Comparison.STARTS_WITH) {
//            if(browserName.startsWith(inputValue)) {
//                return true;
//            }
//        } else if(comparison == Comparison.ENDS_WITH) {
//            if(browserName.endsWith(inputValue)) {
//                return true;
//            }
//        } else if(comparison == Comparison.CONTAINS) {
//            if(browserName.contains(inputValue)) {
//                return true;
//            }
//        } else if(comparison == Comparison.REGEX) {
//            Pattern pattern = Pattern.compile(inputValue);
//            Matcher matcher = pattern.matcher(browserName);
//            if(matcher.find()) {
//                return true;
//            }
//        }
		return false;
	}

    @Override
    public Instance instanceFrom(Comparison comparison, List<ParameterModel> values) {
        return new Instance(comparison, values);
    }

    public static class Instance implements RuleComponentInstance {

        private Instance(Comparison comparison, List<ParameterModel> values) {
        }
    }
}
