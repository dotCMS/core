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

import com.dotcms.enterprise.cluster.action.business.ServerActionFactory;
import com.dotcms.enterprise.cluster.action.business.ServerActionFactoryImpl;
import com.dotcms.enterprise.cluster.action.model.ServerActionBean;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.exception.DotDataException;

/**
 * Proxy for obfuscated ServerActionFactoryImpl
 * 
 * @author Oscar Arrieta
 *
 */
public class ServerActionFactoryImplProxy extends ParentProxy implements
		ServerActionFactory {

	private ServerActionFactory serverActionFactoryImpl = null;

	/**
	 * Returns a single instance of the {@link ServerActionFactory} class.
	 * 
	 * @return A single instance of the Server Action Factory.
	 */
	private ServerActionFactory getServerActionFactoryInstance() {
		if (this.serverActionFactoryImpl == null) {
			this.serverActionFactoryImpl = new ServerActionFactoryImpl();
		}
		return this.serverActionFactoryImpl;
	}

	@Override
	protected int[] getAllowedVersions() {
		return new int[]{LicenseLevel.PROFESSIONAL.level,LicenseLevel.PRIME.level,LicenseLevel.PLATFORM.level};
	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionFactory#findById(java.lang.String)
	 */
	@Override
	public ServerActionBean findById(String id) throws DotDataException {
		
		if(allowExecution()){
			return getServerActionFactoryInstance().findById(id);
		}else{
			return new ServerActionBean();
		}
	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionFactory#existsID(java.lang.String)
	 */
	@Override
	public boolean existsID(String id) throws DotDataException {
		
		if(allowExecution()){
			return getServerActionFactoryInstance().existsID(id);
		}else{
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionFactory#save(com.dotcms.enterprise.cluster.action.model.ServerActionBean)
	 */
	@Override
	public ServerActionBean save(ServerActionBean serverActionBean)
			throws DotDataException {
		
		if(allowExecution()){
			return getServerActionFactoryInstance().save(serverActionBean);
		}else{
			return new ServerActionBean();
		}
	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionFactory#update(com.dotcms.enterprise.cluster.action.model.ServerActionBean)
	 */
	@Override
	public ServerActionBean update(ServerActionBean serverActionBean)
			throws DotDataException {
		
		if(allowExecution()){
			return getServerActionFactoryInstance().update(serverActionBean);
		}else{
			return new ServerActionBean();
		}
	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionFactory#delete(com.dotcms.enterprise.cluster.action.model.ServerActionBean)
	 */
	@Override
	public void delete(ServerActionBean serverActionBean)
			throws DotDataException {
		
		if(allowExecution()){
			getServerActionFactoryInstance().delete(serverActionBean);
		}else{
			return;
		}

	}

	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.cluster.action.business.ServerActionFactory#getAllServerActions()
	 */
	@Override
	public List<ServerActionBean> getAllServerActions() throws DotDataException {
		
		if(allowExecution()){
			return getServerActionFactoryInstance().getAllServerActions();
		}else{
			return new ArrayList<>();
		}
	}

}
