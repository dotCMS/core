package com.dotmarketing.portlets.rules.conditionlet;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

/**
 * This conditionlet will allow dotCMS users to check whether the user that
 * issued the request is currently logged in or not. The login status of a user
 * is in the {@link HttpServletRequest} object, which is used to perform the
 * validation and is retrieved using our own API. This {@link Conditionlet}
 * provides a single drop-down menu with the available comparison mechanisms,
 * and it will check the login status with the back-end system, no other user
 * input is to be required or validated.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 05-14-2015
 *
 */
public class UsersLogInConditionlet extends Conditionlet {

	private static final long serialVersionUID = 1L;

	private static final String CONDITIONLET_NAME = "Current User's Log In Status";

	private static final String COMPARISON_IS = "is";
	private static final String COMPARISON_ISNOT = "isNot";

	private LinkedHashSet<Comparison> comparisons = null;

	public UsersLogInConditionlet() {
		super(CONDITIONLET_NAME);
	}

	@Override
	public Set<Comparison> getComparisons() {
		if (this.comparisons == null) {
			this.comparisons = new LinkedHashSet<Comparison>();
			this.comparisons.add(new Comparison(COMPARISON_IS, "Is Logged In"));
			this.comparisons.add(new Comparison(COMPARISON_ISNOT,
					"Is Not Logged In"));
		}
		return this.comparisons;
	}

	@Override
	public ValidationResults validate(Comparison comparison,
			Set<ConditionletInputValue> inputValues) {
		ValidationResults results = new ValidationResults();
		return results;
	}

	@Override
	protected ValidationResult validate(Comparison comparison,
			ConditionletInputValue inputValue) {
		ValidationResult validationResult = new ValidationResult();
		validationResult.setValid(true);
		return validationResult;
	}

	@Override
	public Collection<ConditionletInput> getInputs(String comparisonId) {
		return null;
	}

	@Override
	public boolean evaluate(HttpServletRequest request,
			HttpServletResponse response, String comparisonId,
			List<ConditionValue> values) {
		if (!UtilMethods.isSet(comparisonId)) {
			return false;
		}
		Comparison comparison = getComparisonById(comparisonId);
		if (comparison == null) {
			return false;
		}
		boolean mustBeLoggedIn = (comparisonId.equalsIgnoreCase(COMPARISON_IS) ? true
				: false);
		try {
			User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
			if ((mustBeLoggedIn && user != null)
					|| (!mustBeLoggedIn && user == null)) {
				return true;
			}
		} catch (DotRuntimeException | PortalException | SystemException e) {
			Logger.error(this,
					"Could not retrieved logged-in user from request: "
							+ request.getRequestURL());
		}
		return false;
	}

}
