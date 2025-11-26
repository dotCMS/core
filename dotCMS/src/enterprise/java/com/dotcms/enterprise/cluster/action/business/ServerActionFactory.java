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

import com.dotcms.enterprise.cluster.action.model.ServerActionBean;
import com.dotmarketing.exception.DotDataException;

/**
 * Handle transactions between database and ServerAction.
 * 
 * @author Oscar Arrieta
 *
 */
public interface ServerActionFactory {
	
	/**
	 * Search database for a row that match id.
	 * 
	 * @param id
	 * @return ServerActionBean found by ID.
	 * @throws DotDataException
	 */
	public ServerActionBean findById(String id) throws DotDataException;;
	
	/**
	 * Check if the ID exists in the Database
	 * 
	 * @param id
	 * @return TRUE if the ID is already assigned to another row in the database.
	 * @throws DotDataException
	 */
	public boolean existsID(String id) throws DotDataException;
	
	/**
	 * Saves ServerActionBean in table cluster_server_action.
	 * 
	 * @param serverActionBean
	 * @throws DotDataException
	 */
	public ServerActionBean save(ServerActionBean serverActionBean) throws DotDataException;
	
	/**
	 * Updates ServerActionBean in table cluster_server_action.
	 * 
	 * @param serverActionBean
	 * @throws DotDataException
	 */
	public ServerActionBean update(ServerActionBean serverActionBean) throws DotDataException;
	
	/**
	 * Deletes ServerActionBean in table cluster_server_action, based on ServerActionBean.id.
	 * 
	 * @param serverActionBean
	 * @throws DotDataException
	 */
	public void delete(ServerActionBean serverActionBean) throws DotDataException;
	
	/**
	 * @return All ServerActionBean in table cluster_server_action.
	 * @throws DotDataException
	 */
	public List<ServerActionBean> getAllServerActions() throws DotDataException;

}
