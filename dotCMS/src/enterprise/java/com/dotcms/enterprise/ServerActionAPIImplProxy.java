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

import java.util.ArrayList;
import java.util.List;

import com.dotcms.enterprise.cluster.action.ServerAction;
import com.dotcms.enterprise.cluster.action.business.ServerActionAPI;
import com.dotcms.enterprise.cluster.action.business.ServerActionAPIImpl;
import com.dotcms.enterprise.cluster.action.model.ServerActionBean;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.exception.DotDataException;

/**
 * Proxy for obfuscated ServerActionAPIImpl.
 * 
 * @author Oscar Arrieta
 *
 */
public class ServerActionAPIImplProxy extends ParentProxy implements
		ServerActionAPI {

	private ServerActionAPI serverActionAPIImpl = null;

	/**
	 * Returns a single instance of the {@link ServerActionAPIImpl} class.
	 * 
	 * @return A single instance of the Server Action API.
	 */
	private ServerActionAPI getServerActionAPIInstance() {
		if (this.serverActionAPIImpl == null) {
			this.serverActionAPIImpl = new ServerActionAPIImpl();
		}
		return this.serverActionAPIImpl;
	}

	@Override
	protected int[] getAllowedVersions() {
		return new int[]{LicenseLevel.PROFESSIONAL.level,LicenseLevel.PRIME.level,LicenseLevel.PLATFORM.level};
	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionAPI#getAllServerActions()
	 */
	@Override
	public List<ServerAction> getAllServerActions() {
		
		if(allowExecution()){
			return getServerActionAPIInstance().getAllServerActions();
		}else{
			return new ArrayList<>();
		}
	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionAPI#handleServerAction(com.dotcms.enterprise.cluster.action.model.ServerActionBean)
	 */
	@Override
	public ServerActionBean handleServerAction(ServerActionBean serverActionBean)
			throws DotDataException {
		
		if(allowExecution()){
			return getServerActionAPIInstance().handleServerAction(serverActionBean);
		}else{
			return new ServerActionBean();
		}
	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionAPI#deleteServerActionBean(com.dotcms.enterprise.cluster.action.model.ServerActionBean)
	 */
	@Override
	public void deleteServerActionBean(ServerActionBean serverActionBean)
			throws DotDataException {
		
		if(allowExecution()){
			getServerActionAPIInstance().deleteServerActionBean(serverActionBean);
		}else{
			return;
		}

	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionAPI#deleteServerActionBeans(java.util.List)
	 */
	@Override
	public void deleteServerActionBeans(List<ServerActionBean> serverActionBeans)
			throws DotDataException {
		
		if(allowExecution()){
			getServerActionAPIInstance().deleteServerActionBeans(serverActionBeans);
		}else{
			return;
		}

	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionAPI#getNewServerActionBeans()
	 */
	@Override
	public List<ServerActionBean> getNewServerActionBeans()
			throws DotDataException {
		
		if(allowExecution()){
			return getServerActionAPIInstance().getNewServerActionBeans();
		}else{
			return new ArrayList<>();
		}
	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionAPI#getNewServerActionBeans(java.lang.String)
	 */
	@Override
	public List<ServerActionBean> getNewServerActionBeans(String serverID)
			throws DotDataException {
		
		if(allowExecution()){
			return getServerActionAPIInstance().getNewServerActionBeans(serverID);
		}else{
			return new ArrayList<>();
		}
	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionAPI#saveServerActionBean(com.dotcms.enterprise.cluster.action.model.ServerActionBean)
	 */
	@Override
	public ServerActionBean saveServerActionBean(
			ServerActionBean serverActionBean) throws DotDataException {
		
		if(allowExecution()){
			return getServerActionAPIInstance().saveServerActionBean(serverActionBean);
		}else{
			return new ServerActionBean();
		}
	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionAPI#findServerActionBean(java.lang.String)
	 */
	@Override
	public ServerActionBean findServerActionBean(String serverActionBeanID)
			throws DotDataException {
		
		if(allowExecution()){
			return getServerActionAPIInstance().findServerActionBean(serverActionBeanID);
		}else{
			return new ServerActionBean();
		}
		
	}

}
