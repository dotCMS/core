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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import com.dotcms.cluster.bean.Server;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.json.JSONException;

/**
 * Proxy to handle all logic for clusters. Should be called by REST API and Server Action.
 * 
 * @author Oscar Arrieta.
 *
 */
public class ClusterUtilProxy extends ParentProxy {
	
	private static int[] ret = {LicenseLevel.PROFESSIONAL.level,LicenseLevel.PRIME.level, LicenseLevel.PLATFORM.level};
	
	/**
	 * @return JSONObject with Information of the local node.
	 * @throws DotStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws JSONException
	 */
    public static Map<String, Serializable> getNodeInfo() {
		if(allowExecution(getStaticAllowedVersions())){
			return ClusterUtil.getNodeInfo();
		} else {
			return new HashMap<>();
		}
	}
	
	/**
	 * @param server
	 * @return JSONObject with default fields filled. Intended to use when the Communications or Process fail.
	 * @throws JSONException
	 * @throws DotDataException 
	 */
    public static HashMap<String,Serializable> createFailedJson(final Server server) {
		if(allowExecution(getStaticAllowedVersions())){
			return ClusterUtil.createFailedJson(server);
		} else {
	         return new HashMap<>();
		}
	}
	
	protected static int[] getStaticAllowedVersions() {
		return ret;
	}
}
