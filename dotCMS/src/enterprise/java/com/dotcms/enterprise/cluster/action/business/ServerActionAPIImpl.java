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
package com.dotcms.enterprise.cluster.action.business;

import java.util.ArrayList;
import java.util.List;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.cluster.action.NodeStatusServerAction;
import com.dotcms.enterprise.cluster.action.ResetLicenseServerAction;
import com.dotcms.enterprise.cluster.action.ServerAction;
import com.dotcms.enterprise.cluster.action.model.ServerActionBean;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;

/**
 * @author Oscar Arrieta
 *
 */
public class ServerActionAPIImpl implements ServerActionAPI {
	
	final static ServerActionFactory serverActionFactory = FactoryLocator.getServerActionFactory();
	
	/**
	 * Iterates over all the Implementations of ServerAction to find a match.
	 * 
	 * @param serverActionID
	 * @return ServerAction that matches serverActionID, null if it is not found.
	 */
	private ServerAction getServerAction(String serverActionID){
		
		ServerAction returnedServerAction = null;
		
		for (ServerAction serverActionIter : getAllServerActions()) {
			if(serverActionIter.getServerActionID().equals(serverActionID)){
				returnedServerAction = serverActionIter;
				break;
			}
		}
		
		return returnedServerAction;
	}
	
	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionAPI#getAllServerActions()
	 */
	@CloseDBIfOpened
	@Override
	public List<ServerAction> getAllServerActions() {
		
		List<ServerAction> serverActions = new ArrayList<>();
		
		serverActions.add(new NodeStatusServerAction());
		serverActions.add(new ResetLicenseServerAction());
		
		return serverActions;
	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionAPI#handleServerAction(com.dotcms.enterprise.cluster.action.model.ServerActionBean)
	 */
	@Override
	@WrapInTransaction
	public ServerActionBean handleServerAction(ServerActionBean serverActionBean) throws DotDataException {
		ServerAction serverAction = getServerAction(serverActionBean.getServerActionId());
		JSONObject response = serverAction.run();

		//If we have and error and we have the response.
		if(response.has(ServerAction.ERROR_STATE)){
			serverActionBean.setResponse(response);
			serverActionBean.setFailed(true);

		//If everything is OK and we have the response.
		} else {
			serverActionBean.setResponse(response);
			serverActionBean.setFailed(false);
		}
		serverActionBean.setCompleted(true);

		serverActionFactory.update(serverActionBean);

		return serverActionBean;
	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionAPI#deleteServerActionBean(com.dotcms.enterprise.cluster.action.model.ServerActionBean)
	 */
	@WrapInTransaction
	@Override
	public void deleteServerActionBean(ServerActionBean serverActionBean) throws DotDataException {
		
		serverActionFactory.delete(serverActionBean);
	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionAPI#deleteServerActionBeans(java.util.List)
	 */
	// todo: should be this a transaction (all or nothing)???
	@Override
	public void deleteServerActionBeans(List<ServerActionBean> serverActionBeans) throws DotDataException {
		
		for (ServerActionBean serverActionBean : serverActionBeans) {
			this.deleteServerActionBean(serverActionBean);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionAPI#getNewServerActionBeans()
	 */
	@CloseDBIfOpened
	@Override
	public List<ServerActionBean> getNewServerActionBeans() throws DotDataException {
		
		List<ServerActionBean> list = serverActionFactory.getAllServerActions();
		List<ServerActionBean> returnedlist = new ArrayList<>();
		
		for (ServerActionBean serverActionBean : list) {
			if(!serverActionBean.isCompleted()){
				returnedlist.add(serverActionBean);
			}
		}
		
		return returnedlist;
	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionAPI#getNewServerActionBeans(java.lang.String)
	 */
	@Override
	@CloseDBIfOpened
	public List<ServerActionBean> getNewServerActionBeans(final String serverID)
			throws DotDataException {

		final List<ServerActionBean> list = serverActionFactory.getAllServerActions();
		final List<ServerActionBean> returnedlist = new ArrayList<>();

		if(UtilMethods.isSet(serverID)){
			for (ServerActionBean serverActionBean : list) {
				if(!serverActionBean.isCompleted() && serverActionBean.getServerId().equals(serverID)){
					returnedlist.add(serverActionBean);
				}
			}
		} else {
			Logger.error(ServerActionAPIImpl.class,
					"Error calling getNewServerActionBeans because serverID is null.");
		}

		return returnedlist;
	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionAPI#saveServerActionBean(com.dotcms.enterprise.cluster.action.model.ServerActionBean)
	 */
	@WrapInTransaction
	@Override
	public ServerActionBean saveServerActionBean(ServerActionBean serverActionBean)
			throws DotDataException {
		
		return serverActionFactory.save(serverActionBean);
	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionAPI#findServerActionBean(java.lang.String)
	 */
	@CloseDBIfOpened
	@Override
	public ServerActionBean findServerActionBean(String serverActionBeanID)
			throws DotDataException {
		
		return serverActionFactory.findById(serverActionBeanID);
	}

}
