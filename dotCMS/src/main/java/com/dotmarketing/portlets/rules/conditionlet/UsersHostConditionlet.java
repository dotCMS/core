//package com.dotmarketing.portlets.rules.conditionlet;
//
//import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
//import com.dotcms.repackage.com.google.common.collect.Sets;
//import com.dotmarketing.portlets.rules.RuleComponentInstance;
//import com.dotmarketing.portlets.rules.ValidationResult;
//import com.dotmarketing.portlets.rules.model.ParameterModel;
//import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
//import java.util.Collection;
//import java.util.LinkedHashMap;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.regex.Pattern;
//import java.util.regex.PatternSyntaxException;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import com.dotmarketing.beans.Host;
//import com.dotmarketing.business.web.WebAPILocator;
//import com.dotmarketing.exception.DotDataException;
//import com.dotmarketing.exception.DotSecurityException;
//import com.dotmarketing.util.Logger;
//import com.dotmarketing.util.UtilMethods;
//import com.liferay.portal.PortalException;
//import com.liferay.portal.SystemException;
//
//import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
//
///**
// * This conditionlet will allow dotCMS users to check the host name a user
// * request is directed to. The information is obtained by extracting the host
// * information the {@link HttpServletRequest} object using our own API. The
// * comparison of host names is case-insensitive, except for the regular
// * expression comparison. This {@link Conditionlet} provides a drop-down menu
// * with the available comparison mechanisms, and a single text field to enter
// * the value to compare.
// *
// * @author Jose Castro
// * @version 1.0
// * @since 04-20-2015
// *
// */
//public class UsersHostConditionlet extends Conditionlet<UsersHostConditionlet.Instance> {
//
//	private static final long serialVersionUID = 1L;
//
//	private static final String INPUT_ID = "host";
//	private static final String CONDITIONLET_NAME = "User's Host";
//
//	private static final String COMPARISON_IS = "is";
//	private static final String COMPARISON_ISNOT = "isNot";
//	private static final String COMPARISON_STARTSWITH = "startsWith";
//	private static final String COMPARISON_ENDSWITH = "endsWith";
//	private static final String COMPARISON_CONTAINS = "contains";
//	private static final String COMPARISON_REGEX = "regex";
//
//	private LinkedHashSet<Comparison> comparisons = null;
//	private Map<String, ConditionletInput> inputValues = null;
//
//	public UsersHostConditionlet() {
//        super("api.system.ruleengine.conditionlet.VisitorHost", ImmutableSet.<Comparison>of(IS,
//                                                                                  Comparison.IS_NOT,
//                                                                                  Comparison.STARTS_WITH,
//                                                                                  Comparison.ENDS_WITH,
//                                                                                  Comparison.CONTAINS,
//                                                                                  Comparison.REGEX), Sets.newHashSet());
//	}
//
//	protected ValidationResult validate(Comparison comparison,
//			ConditionletInputValue inputValue) {
//		ValidationResult validationResult = new ValidationResult();
//		String inputId = inputValue.getConditionletInputId();
//		if (UtilMethods.isSet(inputId)) {
//			String selectedValue = inputValue.getValue();
//			String comparisonId = comparison.getId();
//			if (comparisonId.equals(COMPARISON_IS)
//					|| comparisonId.equals(COMPARISON_ISNOT)
//					|| comparisonId.equals(COMPARISON_STARTSWITH)
//					|| comparisonId.equals(COMPARISON_ENDSWITH)
//					|| comparisonId.equals(COMPARISON_CONTAINS)) {
//				if (UtilMethods.isSet(selectedValue)) {
//					validationResult.setValid(true);
//				}
//			} else if (comparisonId.equals(COMPARISON_REGEX)) {
//				try {
//					Pattern.compile(selectedValue);
//					validationResult.setValid(true);
//				} catch (PatternSyntaxException e) {
//					Logger.debug(this, "Invalid RegEx " + selectedValue);
//				}
//			}
//			if (!validationResult.isValid()) {
//				validationResult.setErrorMessage("Invalid value for input '"
//						+ inputId + "': '" + selectedValue + "'");
//			}
//		}
//		return validationResult;
//	}
//
//	@Override
//	public Collection<ConditionletInput> getInputs(String comparisonId) {
//		if (this.inputValues == null) {
//			ConditionletInput inputField = new ConditionletInput();
//			// Set field configuration and available options
//			inputField.setId(INPUT_ID);
//			inputField.setUserInputAllowed(true);
//			inputField.setMultipleSelectionAllowed(false);
//			this.inputValues = new LinkedHashMap<String, ConditionletInput>();
//			this.inputValues.put(inputField.getId(), inputField);
//		}
//		return this.inputValues.values();
//	}
//
//	@Override
//    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
//
////        String hostName = getHostName(request);
////		Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
////		String inputValue = values.get(0).getValue();
////		inputValues.add(new ConditionletInputValue(INPUT_ID, inputValue));
////		ValidationResults validationResults = validate(comparison, inputValues);
////		if (validationResults.hasErrors()) {
////			return false;
////		}
////		if (!comparison.getId().equals(COMPARISON_REGEX)) {
////			hostName = hostName.toLowerCase();
////			inputValue = inputValue.toLowerCase();
////		}
////		if (comparison.getId().equals(COMPARISON_IS)) {
////			if (hostName.equalsIgnoreCase(inputValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_ISNOT)) {
////			if (!hostName.equalsIgnoreCase(inputValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_STARTSWITH)) {
////			if (hostName.startsWith(inputValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_ENDSWITH)) {
////			if (hostName.endsWith(inputValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_CONTAINS)) {
////			if (hostName.contains(inputValue)) {
////				return true;
////			}
////		} else if (comparison.getId().equals(COMPARISON_REGEX)) {
////			Pattern pattern = Pattern.compile(inputValue);
////			Matcher matcher = pattern.matcher(hostName);
////			if (matcher.find()) {
////				return true;
////			}
////		}
//		return false;
//	}
//
//	/**
//	 * Returns the name of the site (host) based on the
//	 * {@code HttpServletRequest} object.
//	 *
//	 * @param request
//	 *            - The {@code HttpServletRequest} object.
//	 * @return The name of the site, or {@code null} if the site information is
//	 *         not available.
//	 */
//	private String getHostName(HttpServletRequest request) {
//		try {
//			Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
//			if (host != null) {
//				return host.getHostname();
//			}
//		} catch (PortalException | SystemException | DotDataException
//				| DotSecurityException e) {
//			Logger.error(this,
//					"Could not retrieve current host information for: "
//							+ request.getRequestURL());
//		}
//		return null;
//	}
//
//    @Override
//    public Instance instanceFrom(Comparison comparison, List<ParameterModel> values) {
//        return new Instance(comparison, values);
//    }
//
//    public static class Instance implements RuleComponentInstance {
//
//        private Instance(Comparison comparison, List<ParameterModel> values) {
//        }
//    }
//
//}
