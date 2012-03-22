/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.ejb;

import com.liferay.portal.model.User;

/**
 * <a href="UserLocalManagerUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.70 $
 *
 */
public class UserLocalManagerUtil {
	public static com.liferay.portal.model.User addUser(
		java.lang.String companyId, boolean autoUserId,
		java.lang.String userId, boolean autoPassword,
		java.lang.String password1, java.lang.String password2,
		boolean passwordReset, java.lang.String firstName,
		java.lang.String middleName, java.lang.String lastName,
		java.lang.String nickName, boolean male, java.util.Date birthday,
		java.lang.String emailAddress, java.util.Locale locale)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserLocalManager userLocalManager = UserLocalManagerFactory.getManager();

			return userLocalManager.addUser(companyId, autoUserId, userId,
				autoPassword, password1, password2, passwordReset, firstName,
				middleName, lastName, nickName, male, birthday, emailAddress,
				locale);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static void deleteUser(java.lang.String userId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserLocalManager userLocalManager = UserLocalManagerFactory.getManager();
			userLocalManager.deleteUser(userId);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static java.util.List findByC_SMS(java.lang.String companyId)
		throws com.liferay.portal.SystemException {
		try {
			UserLocalManager userLocalManager = UserLocalManagerFactory.getManager();

			return userLocalManager.findByC_SMS(companyId);
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static com.liferay.portal.model.User getDefaultUser(
		java.lang.String companyId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserLocalManager userLocalManager = UserLocalManagerFactory.getManager();

			return userLocalManager.getDefaultUser(companyId);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static com.liferay.portal.model.User getUserByEmailAddress(
		java.lang.String companyId, java.lang.String emailAddress)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserLocalManager userLocalManager = UserLocalManagerFactory.getManager();

			return userLocalManager.getUserByEmailAddress(companyId,
				emailAddress);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static com.liferay.portal.model.User getUserById(
		java.lang.String userId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserLocalManager userLocalManager = UserLocalManagerFactory.getManager();

			return userLocalManager.getUserById(userId);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static com.liferay.portal.model.User getUserById(
		java.lang.String companyId, java.lang.String userId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserLocalManager userLocalManager = UserLocalManagerFactory.getManager();

			return userLocalManager.getUserById(companyId, userId);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}


	public static com.liferay.portal.model.User updateActive(
		java.lang.String userId, boolean active)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserLocalManager userLocalManager = UserLocalManagerFactory.getManager();

			return userLocalManager.updateActive(userId, active);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}
	
	public static com.liferay.portal.model.User updateUser(User user) throws com.liferay.portal.PortalException,com.liferay.portal.SystemException {
		try {
			UserLocalManager userLocalManager = UserLocalManagerFactory.getManager();

			return userLocalManager.updateUser(user);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}
	
	public static com.liferay.portal.model.User updateUser(
		java.lang.String userId, java.lang.String password1,
		java.lang.String password2, boolean passwordReset)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserLocalManager userLocalManager = UserLocalManagerFactory.getManager();

			return userLocalManager.updateUser(userId, password1, password2,
				passwordReset);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static com.liferay.portal.model.User updateUser(
		java.lang.String userId, java.lang.String password,
		java.lang.String firstName, java.lang.String middleName,
		java.lang.String lastName, java.lang.String nickName, boolean male,
		java.util.Date birthday, java.lang.String emailAddress,
		java.lang.String smsId, java.lang.String aimId, java.lang.String icqId,
		java.lang.String msnId, java.lang.String ymId,
		java.lang.String favoriteActivity, java.lang.String favoriteBibleVerse,
		java.lang.String favoriteFood, java.lang.String favoriteMovie,
		java.lang.String favoriteMusic, java.lang.String languageId,
		java.lang.String timeZoneId, java.lang.String skinId,
		boolean dottedSkins, boolean roundedSkins, java.lang.String greeting,
		java.lang.String resolution, java.lang.String refreshRate,
		java.lang.String comments)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserLocalManager userLocalManager = UserLocalManagerFactory.getManager();

			return userLocalManager.updateUser(userId, password, firstName,
				middleName, lastName, nickName, male, birthday, emailAddress,
				smsId, aimId, icqId, msnId, ymId, favoriteActivity,
				favoriteBibleVerse, favoriteFood, favoriteMovie, favoriteMusic,
				languageId, timeZoneId, skinId, dottedSkins, roundedSkins,
				greeting, resolution, refreshRate, comments);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static void validate(java.lang.String companyId, boolean autoUserId,
		java.lang.String userId, boolean autoPassword,
		java.lang.String password1, java.lang.String password2,
		java.lang.String firstName, java.lang.String lastName,
		java.lang.String emailAddress)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserLocalManager userLocalManager = UserLocalManagerFactory.getManager();
			userLocalManager.validate(companyId, autoUserId, userId,
				autoPassword, password1, password2, firstName, lastName,
				emailAddress);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static void validate(java.lang.String userId,
		java.lang.String password1, java.lang.String password2)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserLocalManager userLocalManager = UserLocalManagerFactory.getManager();
			userLocalManager.validate(userId, password1, password2);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static void validate(java.lang.String userId,
		java.lang.String firstName, java.lang.String lastName,
		java.lang.String emailAddress, java.lang.String smsId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			UserLocalManager userLocalManager = UserLocalManagerFactory.getManager();
			userLocalManager.validate(userId, firstName, lastName,
				emailAddress, smsId);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}
}