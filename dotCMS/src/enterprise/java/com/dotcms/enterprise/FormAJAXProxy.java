/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included 
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise;

import java.util.HashMap;
import java.util.Map;

import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.priv.FormAJAX;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

public class FormAJAXProxy extends ParentProxy{

	public Map<String, Object> searchFormWidget(String formStructureInode) throws DotDataException, DotSecurityException {
		if(allowExecution()){
			return FormAJAX.searchFormWidget(formStructureInode);
		}else{
			return new HashMap<>();
		}
	}
	
	@Override
	protected int[] getAllowedVersions() {
		return new int[]{LicenseLevel.STANDARD.level, LicenseLevel.PROFESSIONAL.level, LicenseLevel.PRIME.level,
				LicenseLevel.PLATFORM.level};
	}
}
