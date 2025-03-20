/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker.tinymce;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.achecker.ACheckerResponse;
import com.dotcms.enterprise.achecker.model.GuideLineBean;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.api.v1.accessibility.ACheckerResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;

import java.util.List;
import java.util.Map;

/**
 * @deprecated This one and all DWR-related classes will be deprecated in the near future. Please
 * use the REST {@link ACheckerResource} Endpoint instead.
 */
@Deprecated(forRemoval = true)
public class ACheckerDWR {

	private List<GuideLineBean> getListaGudelines() throws Exception {
	    return APILocator.getACheckerAPI().getAccessibilityGuidelineList();
	}

	public List<GuideLineBean> getSupportedGudelines(){	 	
	    if (LicenseUtil.getLevel()<LicenseLevel.STANDARD.level) {
			throw new RuntimeException("need enterprise license");
		}
		try {
			 return  getListaGudelines();
		} catch (final Exception e) {
			Logger.error(this, String.format("Failed to retrieve Accessibility Guidelines: %s",
					ExceptionUtil.getErrorMessage(e)), e);
		}
		return null;		
	}

	public ACheckerResponse validate(final Map<String, String> params) {
	    if (LicenseUtil.getLevel()<LicenseLevel.STANDARD.level) {
			throw new RuntimeException("need enterprise license");
		}
		try {
			return APILocator.getACheckerAPI().validate(params);
		} catch (final Exception e) {
			Logger.error(this, String.format("Failed to validate Accessibility Guidelines in content with params: " +
							"[ %s ] : %s", params, ExceptionUtil.getErrorMessage(e)), e);
		}
		return null;
	}

}
