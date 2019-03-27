package com.dotmarketing.business;

import java.util.HashMap;
import java.util.List;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

/**
 * 
 * @author David Torres
 */
public class UserProxyAPIImpl implements UserProxyAPI {

	private final UserProxyFactory userProxyFactory;
	private final PermissionAPI permissionAPI;
	
	public UserProxyAPIImpl() {
		userProxyFactory = FactoryLocator.getUserProxyFactory();
		permissionAPI = APILocator.getPermissionAPI();
	}

	@CloseDBIfOpened
	public UserProxy getUserProxy(String userId, User user, boolean respectFrontEndRoles) throws DotRuntimeException, DotSecurityException, DotDataException{
		if(userId == null){
			return null;
		}
		UserProxy up = userProxyFactory.getUserProxy(userId);
		
		if(permissionAPI.doesUserHavePermission(up, PermissionAPI.PERMISSION_READ, user, respectFrontEndRoles)){
			return up;
		}else{
			throw new DotSecurityException("User doesn't have permissions to retrieve UserProxy");
		}
	}

	public UserProxy getUserProxy(User userToGetProxyFor, User user, boolean respectFrontEndRoles) throws DotRuntimeException, DotSecurityException, DotDataException{
		return getUserProxy(userToGetProxyFor.getUserId(),user,respectFrontEndRoles);
	}
	
	/*public static UserProxy getUserProxy(long userProxyInode) 
	{		
		return (UserProxy) InodeFactory.getInode(userProxyInode,UserProxy.class);
	}*/
	@CloseDBIfOpened
	public UserProxy getUserProxyByLongLiveCookie(String dotCMSID, User user, boolean respectFrontEndRoles) throws DotRuntimeException, DotSecurityException, DotDataException{
		UserProxy up =  userProxyFactory.getUserProxyByLongLiveCookie(dotCMSID);
		if(permissionAPI.doesUserHavePermission(up, PermissionAPI.PERMISSION_READ, user, respectFrontEndRoles)){
			return up;
		}else{
			throw new DotSecurityException("User doesn't have permissions to retrieve UserProxy");
		}
	}

	@WrapInTransaction
	public void saveUserProxy(UserProxy userProxy, User user, boolean respectFrontEndRoles) throws DotRuntimeException, DotDataException, DotSecurityException
	{
		if(!permissionAPI.doesUserHavePermission(userProxy, PermissionAPI.PERMISSION_EDIT, user, respectFrontEndRoles)){
			throw new DotSecurityException("User doesn't have permission to save the user proxy object which is trying to be saved");
		}
		userProxyFactory.saveUserProxy(userProxy);
	}

	@CloseDBIfOpened
	public List<String> findUsersTitle() throws DotDataException {
		return userProxyFactory.findUsersTitle();
	}

	@CloseDBIfOpened
	public HashMap<String, Object> searchUsersAndUsersProxy(String firstName, String lastName, String title, boolean showUserGroups, List<Role> roles, boolean showUserRoles,String orderBy, int page, int pageSize) throws DotDataException {
		return userProxyFactory.searchUsersAndUsersProxy(firstName, lastName, title, showUserGroups, roles, showUserRoles, orderBy, page, pageSize);
	}
}