/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

/**
 * 
 */
package com.dotcms.enterprise;

import com.dotcms.enterprise.license.LicenseLevel;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.auth.Authenticator;

/**
 * @author jasontesser
 *
 */
public class LDAPProxy extends ParentProxy implements Authenticator {

	/* (non-Javadoc)
	 * @see com.liferay.portal.auth.Authenticator#authenticateByEmailAddress(java.lang.String, java.lang.String, java.lang.String)
	 */
	public int authenticateByEmailAddress(String companyId,
			String emailAddress, String password) throws AuthException {
		if(!allowExecution()){
			return 0;
		}
		LDAPImpl concrete = new LDAPImpl();
		return concrete.authenticateByEmailAddress(companyId, emailAddress, password);
	}

	/* (non-Javadoc)
	 * @see com.liferay.portal.auth.Authenticator#authenticateByUserId(java.lang.String, java.lang.String, java.lang.String)
	 */
	public int authenticateByUserId(String companyId, String userId,
			String password) throws AuthException {
		if(!allowExecution()){
			return 0;
		}
		LDAPImpl concrete = new LDAPImpl();
		return concrete.authenticateByUserId(companyId, userId, password);
	}

	protected int[] getAllowedVersions() {
		int[] ret = {LicenseLevel.STANDARD.level,LicenseLevel.PROFESSIONAL.level,LicenseLevel.PRIME.level,
				LicenseLevel.PLATFORM.level};
		return ret;
	}

}
