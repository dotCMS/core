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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.de.qaware.heimdall.PasswordException;
import com.dotcms.repackage.com.liferay.counter.ejb.CounterManagerUtil;
import com.dotmarketing.util.Logger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.PasswordTracker;
import com.liferay.portal.pwd.RegExpToolkit;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.Encryptor;
import com.liferay.util.GetterUtil;
import com.liferay.util.Time;

/**
 * <a href="PasswordTrackerLocalManagerImpl.java.html"><b><i>View Source</i></b>
 * </a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class PasswordTrackerLocalManagerImpl
	implements PasswordTrackerLocalManager {

	private List<Object> validationErrorsList = null;

	public void deleteAll(String userId) throws SystemException {
		PasswordTrackerUtil.removeByUserId(userId);
	}

	/**
	 * Performs the password validation of a user on different levels. The
	 * configuration of the properties is located in the
	 * {@code portal.properties} file. This method takes into account the
	 * following aspects:
	 * <ul>
	 * <li><b>Password validation:</b> The password MUST meet a defined set of
	 * special characters for it to be valid. These policies are enforced by a
	 * RegEx.</li>
	 * <li><b>Password Recycling:</b> If the password recycling policy is
	 * enabled, users will not be able to reuse their passwords before a given
	 * number of days.</li>
	 * </ul>
	 * 
	 * @param userId
	 *            - The ID of the user setting its password.
	 * @param password
	 *            - The password to be set.
	 * @return If the password meets all the security policies, returns
	 *         {@code true}. Otherwise, returns {@code false} and the respective
	 *         error messages will be set.
	 */
	public boolean isValidPassword(String userId, String password)
		throws PortalException, SystemException {
		RegExpToolkit regExpToolkit = new RegExpToolkit();
		this.validationErrorsList = new ArrayList<Object>();
		boolean successful = true;
		// Validate character rules
		if (!regExpToolkit.validate(password)) {
			this.validationErrorsList.add(regExpToolkit
					.getErrorMessageFromConfig(PropsUtil.PASSWORDS_REGEXPTOOLKIT_PATTERN_ERROR));
			successful = false;
		}
		// Validate recycling
		if (isPasswordRecyclingActive()) {
            String newEncPwd = null;
            // Use new password hash method
            try {
                newEncPwd = PasswordFactoryProxy.generateHash(password);
            } catch (PasswordException e) {
                Logger.error(PasswordTrackerLocalManagerImpl.class,
                        "An error occurred generating the hashed password for userId: " + userId, e);
                throw new SystemException("An error occurred generating the hashed password.");
            }

			Date now = new Date();
			int passwordsRecycle = GetterUtil.getInteger(PropsUtil
					.get(PropsUtil.PASSWORDS_RECYCLE));
			Iterator itr = PasswordTrackerUtil.findByUserId(userId).iterator();
			while (itr.hasNext()) {
				PasswordTracker passwordTracker = (PasswordTracker) itr.next();
				Date recycleDate = new Date(passwordTracker.getCreateDate()
						.getTime() + Time.DAY * passwordsRecycle);
				if (recycleDate.after(now)) {
					if (passwordTracker.getPassword().equals(newEncPwd)) {
						this.validationErrorsList.add(regExpToolkit
								.getErrorMessageFromConfig(PropsUtil.PASSWORDS_RECYCLE_ERROR));
						successful = false;
					}
				}
			}
		}
		return successful;
	}

	public void trackPassword(String userId, String encPwd)
		throws PortalException, SystemException {

		String passwordTrackerId = Long.toString(CounterManagerUtil.increment(
			PasswordTracker.class.getName()));

		PasswordTracker passwordTracker =
			PasswordTrackerUtil.create(passwordTrackerId);

		passwordTracker.setUserId(userId);
		passwordTracker.setCreateDate(new Date());
		passwordTracker.setPassword(encPwd);

		PasswordTrackerUtil.update(passwordTracker);
	}

	@Override
	public boolean isPasswordRecyclingActive() {
		int passwordsRecycle = GetterUtil.getInteger(PropsUtil
				.get(PropsUtil.PASSWORDS_RECYCLE));
		if (passwordsRecycle > 0) {
			return true;
		}
		return false;
	}

	@Override
	public List<Object> getValidationErrors() {
		return this.validationErrorsList;
	}
	
}
