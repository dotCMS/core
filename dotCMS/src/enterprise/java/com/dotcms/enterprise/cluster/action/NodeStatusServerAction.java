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

import java.util.Date;


import com.dotcms.enterprise.ClusterUtilProxy;
import com.dotcms.enterprise.cluster.action.model.ServerActionBean;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

/**
 * @author Oscar Arrieta
 *
 */
public class NodeStatusServerAction implements ServerAction {
	
	public static String ACTION_ID = "NODE_STATUS";
	public static String JSON_NODE_STATUS = "jsonNodeStatusObject";

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.ServerAction#getServerActionID()
	 */
	@Override
	public String getServerActionID() {
		return ACTION_ID;
	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.ServerAction#run()
	 */
	@Override
	public JSONObject run() {
		
		JSONObject jsonObject = new JSONObject();
		
		try {
			JSONObject jsonNodeStatusObject = new JSONObject(ClusterUtilProxy.getNodeInfo());
			
			jsonObject.put(ServerAction.SUCCESS_STATE, "Info from the node gathered");
			jsonObject.put(JSON_NODE_STATUS, jsonNodeStatusObject);
			
			Logger.info(NodeStatusServerAction.class, "Info from the node gathered");
			
		} catch (Exception e) {
			
            try {
            	Logger.error(NodeStatusServerAction.class, "Can NOT get Node Info ", e);
                jsonObject.put(ServerAction.ERROR_STATE, "Can NOT get Node Info");
                
            } catch (JSONException ex) {
            	Logger.error(NodeStatusServerAction.class, "Can NOT write JSONObject ", ex);
			}
		} 
		
		return jsonObject;
	}
	
	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.ServerAction#getNewServerAction(java.lang.String, java.lang.String, java.lang.Long)
	 */
	@Override
	public ServerActionBean getNewServerAction(String originatorServerID, String receptorServerID, Long timeoutSeconds) {
		ServerActionBean serverActionBean = new ServerActionBean();
		serverActionBean.setOriginatorId(originatorServerID);
		serverActionBean.setServerId(receptorServerID);
		serverActionBean.setFailed(false);
		serverActionBean.setResponse(null);
		serverActionBean.setServerActionId(ACTION_ID);
		serverActionBean.setCompleted(false);
		serverActionBean.setEnteredDate(new Date());
		serverActionBean.setTimeOutSeconds(timeoutSeconds);
		
		return serverActionBean;
	}

}
