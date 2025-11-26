/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.velocity;

import org.apache.velocity.runtime.directive.Directive;

import com.dotcms.enterprise.license.LicenseManager;

public abstract class DotDirective extends Directive  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Will return whether or not the functionality should run
	 * @return
	 */
	protected boolean allowExecution(){
		LicenseManager i=LicenseManager.getInstance();
		return i.isAuthorized(getAllowedVersions());
	}
	
	
	protected abstract int[] getAllowedVersions();
	
}
