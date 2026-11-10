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

package com.liferay.portal.auth;

/**
 * <a href="LoginMaxFailures.java.html"><b><i>View Source</i></b></a>
 *
 * <p>Default handler for the {@code auth.max.failures} extension point (see {@link
 * com.liferay.portal.ejb.UserManagerImpl#runCustomMaxFailuresHandlers}), run whenever an account's failed
 * logins reach {@code auth.max.failures.limit}. Both methods below are empty: out of the box this does
 * nothing, including no lockout or account disabling. It exists only so a deployment can plug in its own
 * {@link AuthFailure} here (notify an admin, disable the account, etc.) without touching dotCMS's own code.
 *
 * <p>Account lockout itself is decided by {@link com.dotcms.auth.LoginLockout}, which runs independently of
 * this hook and does not call it. Do not look here for the lockout logic.
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.5 $
 *
 */
public class LoginMaxFailures implements AuthFailure {

	public void onFailureByEmailAddress(String companyId, String emailAddress)
		throws AuthException {
	}

	public void onFailureByUserId(String companyId, String userId)
		throws AuthException {
	}

}