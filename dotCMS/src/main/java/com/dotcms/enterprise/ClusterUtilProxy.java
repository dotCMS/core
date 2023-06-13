/**
 * 
 */
package com.dotcms.enterprise;

import com.dotcms.cluster.bean.Server;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

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
	public static JSONObject getNodeInfo() throws DotStateException, DotDataException, DotSecurityException, JSONException, DotCacheException {
		if(allowExecution(getStaticAllowedVersions())){
			return ClusterUtil.getNodeInfo();
		} else {
			return new JSONObject();
		}
	}
	
	/**
	 * @param server
	 * @return JSONObject with default fields filled. Intended to use when the Communications or Process fail.
	 * @throws JSONException
	 * @throws DotDataException 
	 */
	public static JSONObject createFailedJson(Server server) throws JSONException, DotDataException{
		if(allowExecution(getStaticAllowedVersions())){
			return ClusterUtil.createFailedJson(server);
		} else {
			return new JSONObject();
		}
	}
	
	protected static int[] getStaticAllowedVersions() {
		return ret;
	}
}
