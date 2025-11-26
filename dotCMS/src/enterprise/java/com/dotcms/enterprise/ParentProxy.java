/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise;

import com.dotcms.enterprise.license.LicenseManager;

public abstract class ParentProxy {

	/**
	 * Will return whether or not the functionality should run
	 * @return
	 */
	protected boolean allowExecution(){
		LicenseManager i=LicenseManager.getInstance();
		return i.isAuthorized(getAllowedVersions());
	}
	
	/**
	 * Will return whether or not the functionality should run
	 * @param allowedVersions
	 * @return
	 */
	protected static boolean allowExecution(int[] allowedVersions){
		LicenseManager i=LicenseManager.getInstance();
		return i.isAuthorized(allowedVersions);
	}
	
	protected int[] getAllowedVersions() {
		return null;
	}
	
}
