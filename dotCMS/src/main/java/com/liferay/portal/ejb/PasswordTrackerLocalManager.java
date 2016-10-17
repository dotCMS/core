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

import java.util.List;

/**
 * Provides utility routines to deal with the password security policies
 * specified for the server, which includes:
 * <ul>
 * <li>Character and length validation.</li>
 * <li>Recycling validation.</li>
 * </ul>
 *
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public interface PasswordTrackerLocalManager {
	/**
	 * Deletes the complete password history for a specific user.
	 * 
	 * @param userId
	 *            - The ID of the user whose records will be deleted.
	 * @throws com.liferay.portal.SystemException
	 */
	public void deleteAll(java.lang.String userId)
		throws com.liferay.portal.SystemException;

	/**
	 * Validates the password of a given user according to the portal security
	 * settings. If validation errors are present, they can be retrieved through
	 * the {@link #getValidationErrors()} method.
	 * 
	 * @param userId
	 *            - The ID of the user whose password is being validated.
	 * @param password
	 *            - The human-readable password.
	 * @return If the password contains valid characters, the correct length
	 *         and, if applicable, it is not recycled, returns <code>true</code>
	 *         .
	 * @throws PortalException
	 * @throws SystemException
	 */
	public boolean isValidPassword(java.lang.String userId,
		java.lang.String password)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException;

	/**
	 * Keeps track of a valid password that has been set for a given user. This
	 * allows the password recycle process to correctly validate password (if
	 * activated).
	 * 
	 * @param userId
	 *            - The ID of the user whose password is being tracked.
	 * @param encPwd
	 *            - The encrypted user password.
	 * @throws PortalException
	 * @throws SystemException
	 */
	public void trackPassword(java.lang.String userId, java.lang.String encPwd)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException;
	
	/**
	 * Indicates whether the password recycling policy is active or not.
	 * 
	 * @return If the password recycling policy is enabled, returns
	 *         <code>true</code>.
	 */
	public boolean isPasswordRecyclingActive();

	/**
	 * Returns a list containing the security checks that failed during the
	 * password validation process.
	 * 
	 * @return A simple {@link List} of errors. An empty list means the
	 *         validation has been successful.
	 */
	public List<Object> getValidationErrors();

}
