/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
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
