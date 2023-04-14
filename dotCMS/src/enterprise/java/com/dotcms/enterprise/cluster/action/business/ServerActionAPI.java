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
