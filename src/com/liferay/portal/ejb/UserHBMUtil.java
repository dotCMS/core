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

/**
 * <a href="UserHBMUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class UserHBMUtil {
	public static com.liferay.portal.model.User model(UserHBM userHBM) {
		com.liferay.portal.model.User user = UserPool.get(userHBM.getPrimaryKey());

		if (user == null) {
			user = new com.liferay.portal.model.User(userHBM.getUserId(),
					userHBM.getCompanyId(), userHBM.getPassword(),
					userHBM.getPasswordEncrypted(),
					userHBM.getPasswordExpirationDate(),
					userHBM.getPasswordReset(), userHBM.getFirstName(),
					userHBM.getMiddleName(), userHBM.getLastName(),
					userHBM.getNickName(), userHBM.getMale(),
					userHBM.getBirthday(), userHBM.getEmailAddress(),
					userHBM.getSmsId(), userHBM.getAimId(), userHBM.getIcqId(),
					userHBM.getMsnId(), userHBM.getYmId(),
					userHBM.getFavoriteActivity(),
					userHBM.getFavoriteBibleVerse(), userHBM.getFavoriteFood(),
					userHBM.getFavoriteMovie(), userHBM.getFavoriteMusic(),
					userHBM.getLanguageId(), userHBM.getTimeZoneId(),
					userHBM.getSkinId(), userHBM.getDottedSkins(),
					userHBM.getRoundedSkins(), userHBM.getGreeting(),
					userHBM.getResolution(), userHBM.getRefreshRate(),
					userHBM.getLayoutIds(), userHBM.getComments(),
					userHBM.getCreateDate(), userHBM.getLoginDate(),
					userHBM.getLoginIP(), userHBM.getLastLoginDate(),
					userHBM.getLastLoginIP(), userHBM.getFailedLoginAttempts(),
					userHBM.getAgreedToTermsOfUse(), userHBM.getActive());
			UserPool.put(user.getPrimaryKey(), user);
		}

		return user;
	}
}