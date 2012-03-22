/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.ejb;

import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

/**
 * <a href="UserLocalManager.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.69 $
 *
 */
public interface UserLocalManager {
//	public boolean addGroup(java.lang.String userId, java.lang.String groupId)
//		throws com.liferay.portal.PortalException, 
//			com.liferay.portal.SystemException;

//	public boolean addGroup(java.lang.String userId,
//		com.liferay.portal.model.Group group)
//		throws com.liferay.portal.PortalException, 
//			com.liferay.portal.SystemException;
//
//	public boolean addRole(java.lang.String userId, java.lang.String roleId)
//		throws com.liferay.portal.PortalException, 
//			com.liferay.portal.SystemException;
//
//	public boolean addRole(java.lang.String userId,
//		com.liferay.portal.model.Role role)
//		throws com.liferay.portal.PortalException, 
//			com.liferay.portal.SystemException;

	public com.liferay.portal.model.User addUser(java.lang.String companyId,
		boolean autoUserId, java.lang.String userId, boolean autoPassword,
		java.lang.String password1, java.lang.String password2,
		boolean passwordReset, java.lang.String firstName,
		java.lang.String middleName, java.lang.String lastName,
		java.lang.String nickName, boolean male, java.util.Date birthday,
		java.lang.String emailAddress, java.util.Locale locale)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException;

//	public boolean deleteGroup(java.lang.String userId, java.lang.String groupId)
//		throws com.liferay.portal.PortalException, 
//			com.liferay.portal.SystemException;

//	public boolean deleteGroup(java.lang.String userId,
//		com.liferay.portal.model.Group group)
//		throws com.liferay.portal.PortalException, 
//			com.liferay.portal.SystemException;
//
//	public boolean deleteRole(java.lang.String userId, java.lang.String roleId)
//		throws com.liferay.portal.PortalException, 
//			com.liferay.portal.SystemException;
//
//	public boolean deleteRole(java.lang.String userId,
//		com.liferay.portal.model.Role role)
//		throws com.liferay.portal.PortalException, 
//			com.liferay.portal.SystemException;

	public void deleteUser(java.lang.String userId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException;

	public java.util.List findByC_SMS(java.lang.String companyId)
		throws com.liferay.portal.SystemException;

	public com.liferay.portal.model.User getDefaultUser(
		java.lang.String companyId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException;

//	public java.util.List getGroups(java.lang.String userId)
//		throws com.liferay.portal.PortalException, 
//			com.liferay.portal.SystemException;
//
//	public java.util.List getGroups(java.lang.String userId, int begin, int end)
//		throws com.liferay.portal.PortalException, 
//			com.liferay.portal.SystemException;

//	public int getGroupsSize(java.lang.String userId)
//		throws com.liferay.portal.SystemException;

//	public java.util.List getRoles(java.lang.String userId)
//		throws com.liferay.portal.PortalException, 
//			com.liferay.portal.SystemException;

//	public java.util.List getRoles(java.lang.String userId, int begin, int end)
//		throws com.liferay.portal.PortalException, 
//			com.liferay.portal.SystemException;

//	public int getRolesSize(java.lang.String userId)
//		throws com.liferay.portal.SystemException;

	public com.liferay.portal.model.User getUserByEmailAddress(
		java.lang.String companyId, java.lang.String emailAddress)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException;

	public com.liferay.portal.model.User getUserById(java.lang.String userId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException;

	public com.liferay.portal.model.User getUserById(
		java.lang.String companyId, java.lang.String userId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException;

//	public boolean hasGroupById(java.lang.String userId,
//		java.lang.String groupId)
//		throws com.liferay.portal.PortalException, 
//			com.liferay.portal.SystemException;
//
//	public boolean hasGroupByName(java.lang.String companyId,
//		java.lang.String userId, java.lang.String name)
//		throws com.liferay.portal.PortalException, 
//			com.liferay.portal.SystemException;

	

//	public void setGroups(java.lang.String userId, java.lang.String[] groupIds)
//		throws com.liferay.portal.PortalException, 
//			com.liferay.portal.SystemException;
//
//	public void setGroups(java.lang.String userId, java.util.List groups)
//		throws com.liferay.portal.PortalException, 
//			com.liferay.portal.SystemException;
//
//	public void setLayouts(java.lang.String userId, java.lang.String[] layoutIds)
//		throws com.liferay.portal.PortalException, 
//			com.liferay.portal.SystemException;
//
//	public void setRoles(java.lang.String userId, java.lang.String[] roleIds)
//		throws com.liferay.portal.PortalException, 
//			com.liferay.portal.SystemException;
//
//	public void setRoles(java.lang.String userId, java.util.List roles)
//		throws com.liferay.portal.PortalException, 
//			com.liferay.portal.SystemException;


	public com.liferay.portal.model.User updateActive(java.lang.String userId,
		boolean active)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException;

	
	public User updateUser(User user) throws PortalException, SystemException;
	
	public com.liferay.portal.model.User updateUser(java.lang.String userId,
		java.lang.String password1, java.lang.String password2,
		boolean passwordReset)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException;

	public com.liferay.portal.model.User updateUser(java.lang.String userId,
		java.lang.String password, java.lang.String firstName,
		java.lang.String middleName, java.lang.String lastName,
		java.lang.String nickName, boolean male, java.util.Date birthday,
		java.lang.String emailAddress, java.lang.String smsId,
		java.lang.String aimId, java.lang.String icqId, java.lang.String msnId,
		java.lang.String ymId, java.lang.String favoriteActivity,
		java.lang.String favoriteBibleVerse, java.lang.String favoriteFood,
		java.lang.String favoriteMovie, java.lang.String favoriteMusic,
		java.lang.String languageId, java.lang.String timeZoneId,
		java.lang.String skinId, boolean dottedSkins, boolean roundedSkins,
		java.lang.String greeting, java.lang.String resolution,
		java.lang.String refreshRate, java.lang.String comments)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException;

	public void validate(java.lang.String companyId, boolean autoUserId,
		java.lang.String userId, boolean autoPassword,
		java.lang.String password1, java.lang.String password2,
		java.lang.String firstName, java.lang.String lastName,
		java.lang.String emailAddress)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException;

	public void validate(java.lang.String userId, java.lang.String password1,
		java.lang.String password2)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException;

	public void validate(java.lang.String userId, java.lang.String firstName,
		java.lang.String lastName, java.lang.String emailAddress,
		java.lang.String smsId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException;
}