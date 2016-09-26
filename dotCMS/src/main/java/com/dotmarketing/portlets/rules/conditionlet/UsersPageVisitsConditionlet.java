//package com.dotmarketing.portlets.rules.conditionlet;
//
//import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
//import com.dotcms.repackage.com.google.common.collect.Sets;
//import com.dotmarketing.portlets.rules.RuleComponentInstance;
//import com.dotmarketing.portlets.rules.ValidationResult;
//import com.dotmarketing.portlets.rules.model.ParameterModel;
//import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
//import java.io.UnsupportedEncodingException;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.LinkedHashMap;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.regex.Pattern;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import com.dotcms.util.HttpRequestDataUtil;
//import com.dotmarketing.beans.Host;
//import com.dotmarketing.business.web.WebAPILocator;
//import com.dotmarketing.exception.DotDataException;
//import com.dotmarketing.exception.DotSecurityException;
//import com.dotmarketing.util.Logger;
//import com.dotmarketing.util.UtilMethods;
//import com.dotmarketing.util.WebKeys;
//import com.liferay.portal.PortalException;
//import com.liferay.portal.SystemException;
//
//import static com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull;
//import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;
//import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
//
///**
// * This conditionlet will allow dotCMS users to check the number of pages that a
// * user has visited during its current session. The information on the visited
// * pages will be available until the user's session ends. This
// * {@link Conditionlet} provides a drop-down menu with the available comparison
// * mechanisms, and a single text field to enter the number of visits to take
// * into account. The user session has a {@link Map} object holding the URLs that
// * the user has visited per site.
// *
// * @author Jose Castro
// * @version 1.0
// * @since 05-11-2015
// *
// */
//public class UsersPageVisitsConditionlet extends Conditionlet<UsersPageVisitsConditionlet.Instance> {
//
//	private static final long serialVersionUID = 1L;
//
//	private static final String INPUT_ID = "number-visited-pages";
//	private static final String CONDITIONLET_NAME = "User's Visited Pages";
//
//	private Map<String, ConditionletInput> inputValues = null;
//
//	public UsersPageVisitsConditionlet() {
//		super(CONDITIONLET_NAME, ImmutableSet.<Comparison>of(IS,
//                                                             Comparison.IS_NOT,
//                                                             Comparison.STARTS_WITH,
//                                                             Comparison.ENDS_WITH,
//                                                             Comparison.CONTAINS,
//                                                             Comparison.REGEX), Sets.newHashSet());
//	}
//
//
//	protected ValidationResult validate(Comparison comparison, ConditionletInputValue inputValue) {
//		ValidationResult validationResult = new ValidationResult();
//		String inputId = inputValue.getConditionletInputId();
//		if (UtilMethods.isSet(inputId)) {
//			String selectedValue = inputValue.getValue();
//			if (Pattern.matches("\\d+", selectedValue)) {
//				validationResult.setValid(true);
//			} else {
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
//			inputField.setId(INPUT_ID);
//			inputField.setUserInputAllowed(true);
//			inputField.setMultipleSelectionAllowed(false);
//			this.inputValues = new LinkedHashMap<String, ConditionletInput>();
//			this.inputValues.put(inputField.getId(), inputField);
//		}
//		return this.inputValues.values();
//	}
//
//    @Override
//    public Instance instanceFrom(Comparison comparison, List<ParameterModel> values) {
//        return new Instance(comparison, values);
//    }
//
//    @Override
//    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
////        Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
////        String inputValue = values.get(0).getValue();
////        inputValues.add(new ConditionletInputValue(INPUT_ID, inputValue));
////        ValidationResults validationResults = validate(comparison, inputValues);
////        if(validationResults.hasErrors()) {
////            return false;
////        }
////        int visitedPages = getTotalVisitedPages(request);
////        int conditionletInput = Integer.parseInt(inputValue);
////        if(comparison.getId().equals(COMPARISON_GREATER_THAN)) {
////            if(visitedPages > conditionletInput) {
////                return true;
////            }
////        } else if(comparison.getId().equals(
////            COMPARISON_GREATER_THAN_OR_EQUAL_TO)) {
////            if(visitedPages >= conditionletInput) {
////                return true;
////            }
////        } else if(comparison.getId().equals(COMPARISON_EQUAL_TO)) {
////            if(visitedPages == conditionletInput) {
////                return true;
////            }
////        } else if(comparison.getId().equals(COMPARISON_LOWER_THAN_OR_EQUAL_TO)) {
////            if(visitedPages <= conditionletInput) {
////                return true;
////            }
////        } else if(comparison.getId().equals(COMPARISON_LOWER_THAN)) {
////            if(visitedPages < conditionletInput) {
////                return true;
////            }
////        }
//        return false;
//    }
//
//	/**
//	 * Retrieves the number of pages that the user has visited in its current
//	 * session under a specific site (host).
//	 *
//	 * @param request
//	 *            - The {@link HttpServletRequest} object.
//	 * @return The number of visited pages.
//	 */
//	private int getTotalVisitedPages(HttpServletRequest request) {
//		String hostId = getHostId(request);
//		if (!UtilMethods.isSet(hostId)) {
//			return 0;
//		}
//		Map<String, Set<String>> visitedUrls = (Map<String, Set<String>>) request
//				.getSession(true).getAttribute(
//						WebKeys.RULES_CONDITIONLET_VISITEDURLS);
//		if (visitedUrls == null) {
//			visitedUrls = new HashMap<String, Set<String>>();
//		}
//		Set<String> urlSet = null;
//		String uri = null;
//		try {
//			uri = HttpRequestDataUtil.getUri(request);
//		} catch (UnsupportedEncodingException e) {
//			Logger.error(this, "Could not retrieved a valid URI from request: "
//					+ request.getRequestURL());
//		}
//		if (!visitedUrls.containsKey(hostId)) {
//			urlSet = new LinkedHashSet<String>();
//		} else {
//			urlSet = visitedUrls.get(hostId);
//		}
//		if (UtilMethods.isSet(uri) && !urlSet.contains(uri)) {
//			urlSet.add(uri);
//			visitedUrls.put(hostId, urlSet);
//			request.getSession(true).setAttribute(
//					WebKeys.RULES_CONDITIONLET_VISITEDURLS, visitedUrls);
//		}
//		return urlSet.size();
//	}
//
//	/**
//	 * Returns the ID of the site (host) based on the {@code HttpServletRequest}
//	 * object.
//	 *
//	 * @param request
//	 *            - The {@code HttpServletRequest} object.
//	 * @return The ID of the site, or {@code null} if an error occurred when
//	 *         retrieving the site information.
//	 */
//	private String getHostId(HttpServletRequest request) {
//		try {
//			Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
//			if (host != null) {
//				return host.getIdentifier();
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
//    public static class Instance implements RuleComponentInstance {
//
//        private Instance(Comparison comparison, List<ParameterModel> values) {
//        }
//    }
//
//}
