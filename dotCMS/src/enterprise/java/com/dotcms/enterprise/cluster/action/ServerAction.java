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
package com.dotcms.enterprise.cluster.action;

import com.dotcms.enterprise.cluster.action.model.ServerActionBean;
import com.dotmarketing.util.json.JSONObject;

/**
 * The purpose of this Interface is to provide a way to send messages through the DB between the servers in the cluster
 * 
 * @author Oscar Arrieta
 *
 */
public interface ServerAction {
	
	public static final String SUCCESS_STATE = "success";
	public static final String ERROR_STATE = "error";
	
	/**
	 * @return ID of the ServerAction.
	 */
	public String getServerActionID();
	
	/**
	 * @return JSONObect with the response.  
	 */
	public JSONObject run();
	
	/**
	 * @param originatorServerID
	 * @param receptorServerID
	 * @param timeoutSeconds
	 * @return ServerActionBean object with the rest of the fields filled.
	 */
	public ServerActionBean getNewServerAction(String originatorServerID, String receptorServerID, Long timeoutSeconds);

}
