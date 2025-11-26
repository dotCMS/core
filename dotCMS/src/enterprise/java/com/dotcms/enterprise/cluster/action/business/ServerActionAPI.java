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

import java.util.List;

import com.dotcms.enterprise.cluster.action.ServerAction;
import com.dotcms.enterprise.cluster.action.model.ServerActionBean;
import com.dotmarketing.exception.DotDataException;

/**
 * API for Cluster Server Actions
 * 
 * @author Oscar Arrieta
 *
 */
public interface ServerActionAPI {
	
	/**
	 * @return All Server Actions in the server. 
	 */
	public List<ServerAction> getAllServerActions();

	/**
	 * Search for the ServerAction and runs it. Handle the response and updates in the Database.
	 * 
	 * @param serverActionBean
	 * @return Updated ServerActionBean with response.
	 * @throws DotDataException
	 */
	public ServerActionBean handleServerAction(ServerActionBean serverActionBean) throws DotDataException;
	
	/**
	 * Deletes a single ServerActionBean
	 * 
	 * @param serverActionBean
	 * @throws DotDataException
	 */
	public void deleteServerActionBean(ServerActionBean serverActionBean) throws DotDataException;
	
	/**
	 * Deletes a list of ServerActionsBeans.
	 * 
	 * @param serverActionBeans
	 * @throws DotDataException
	 */
	public void deleteServerActionBeans(List<ServerActionBean> serverActionBeans) throws DotDataException;
	
	/**
	 * Searches for all ServerActionBean with isCompleted == false.
	 * 
	 * @return List with all ServerActionBean with completed == false.
	 * @throws DotDataException
	 */
	public List<ServerActionBean> getNewServerActionBeans() throws DotDataException;
	
	/**
	 * Searches for all ServerActionBean with isCompleted == false and matches serverID.
	 * 
	 * @param serverID
	 * @return List with all ServerActionBean with completed == false and match serverID.
	 * @throws DotDataException
	 */
	public List<ServerActionBean> getNewServerActionBeans(String serverID) throws DotDataException;
	
	/**
	 * Saves ServerActionBean in the Database.
	 *  
	 * @param serverActionBean
	 * @return ServerActionBean created with ID.
	 * @throws DotDataException
	 */
	public ServerActionBean saveServerActionBean(ServerActionBean serverActionBean) throws DotDataException;
	
	/**
	 * Find by ID through the Database.
	 * 
	 * @param serverActionBeanID
	 * @return ServerActionBean with matched ID passed as argument.
	 * @throws DotDataException
	 */
	public ServerActionBean findServerActionBean(String serverActionBeanID) throws DotDataException;
	
}
