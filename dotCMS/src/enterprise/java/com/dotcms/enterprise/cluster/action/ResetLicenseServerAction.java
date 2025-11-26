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

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.cluster.action.model.ServerActionBean;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

/**
 * @author oarrieta
 *
 */
public class ResetLicenseServerAction implements ServerAction {
	
	public static String ACTION_ID = "RESET_LICENSE";

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
			HibernateUtil.startTransaction();
			LicenseUtil.freeLicenseOnRepo();
			HibernateUtil.commitTransaction();
			
			Logger.info(ResetLicenseServerAction.class, "License From Repo Freed");
			jsonObject.put(ServerAction.SUCCESS_STATE, "License From Repo Freed");
			
		} catch (Exception e) {
			
            try {
            	Logger.error(ResetLicenseServerAction.class, "Can NOT free license ", e);
                jsonObject.put(ServerAction.ERROR_STATE, "Can NOT free license");
                HibernateUtil.rollbackTransaction();
                
            } catch (DotHibernateException dotHibernateException) {
                Logger.warn(ResetLicenseServerAction.class, "Can NOT rollback", dotHibernateException);
            } catch (JSONException ex) {
            	Logger.error(ResetLicenseServerAction.class, "Can NOT write JSONObject ", ex);
			}
		} finally {
			HibernateUtil.closeSessionSilently();
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
