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
