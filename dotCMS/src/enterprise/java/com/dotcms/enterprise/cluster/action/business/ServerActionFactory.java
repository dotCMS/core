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
