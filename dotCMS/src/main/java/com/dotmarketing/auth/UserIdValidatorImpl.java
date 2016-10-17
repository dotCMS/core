package com.dotmarketing.auth;

import com.liferay.portal.model.User;
import com.liferay.util.Validator;

/**
 * This class is called by the UserLocalManagerUtil to validate a userID.  It is set in either the portal.properties
 * of liferay's jar or the portal-ext.properties of the dotCMS 
 * @author jtesser
 *
 */

public class UserIdValidatorImpl extends com.liferay.portal.UserIdValidator{
	public boolean validate(String userId, String companyId) {
		if (Validator.isNull(userId) ||
//			Validator.isNumber(userId) ||
//			Validator.isEmailAddress(userId) ||
			(userId.equalsIgnoreCase("cyrus")) ||
			(userId.equalsIgnoreCase("postfix")) ||
			(userId.indexOf(User.DEFAULT) != -1)) 
//			(userId.indexOf(companyId) != -1) ||
//			(userId.indexOf("_") != -1)) {
			{
			return false;
		} else {
			return true;
		}
	}
}
