package com.dotmarketing.business;

import java.util.HashMap;
import java.util.List;

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

/**
 * 
 * @author David Torres
 */
public class UserProxyAPIImpl implements UserProxyAPI{

	private UserProxyFactory upf;
	private PermissionAPI perAPI;
	
	public UserProxyAPIImpl() {
		upf = FactoryLocator.getUserProxyFactory();
		perAPI = APILocator.getPermissionAPI();
	}
	
	public UserProxy getUserProxy(String userId, User user, boolean respectFrontEndRoles) throws DotRuntimeException, DotSecurityException, DotDataException{
		if(userId == null){
			return null;
		}
		UserProxy up = upf.getUserProxy(userId);
		
		if(perAPI.doesUserHavePermission(up, PermissionAPI.PERMISSION_READ, user, respectFrontEndRoles)){
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
	
	public UserProxy getUserProxyByLongLiveCookie(String dotCMSID, User user, boolean respectFrontEndRoles) throws DotRuntimeException, DotSecurityException, DotDataException{
		UserProxy up =  upf.getUserProxyByLongLiveCookie(dotCMSID);
		if(perAPI.doesUserHavePermission(up, PermissionAPI.PERMISSION_READ, user, respectFrontEndRoles)){
			return up;
		}else{
			throw new DotSecurityException("User doesn't have permissions to retrieve UserProxy");
		}
	}
	
	public void saveUserProxy(UserProxy userProxy, User user, boolean respectFrontEndRoles) throws DotRuntimeException, DotDataException, DotSecurityException
	{
		if(!perAPI.doesUserHavePermission(userProxy, PermissionAPI.PERMISSION_EDIT, user, respectFrontEndRoles)){
			throw new DotSecurityException("User doesn't have permission to save the user proxy object which is trying to be saved");
		}
		upf.saveUserProxy(userProxy);
	}
	
	public List<String> findUsersTitle() throws DotDataException {
		return upf.findUsersTitle();
	}
	
	public HashMap<String, Object> searchUsersAndUsersProxy(String firstName, String lastName, String title, boolean showUserGroups, List<Role> roles, boolean showUserRoles,String orderBy, int page, int pageSize) throws DotDataException {
		return upf.searchUsersAndUsersProxy(firstName, lastName, title, showUserGroups, roles, showUserRoles, orderBy, page, pageSize);
	}
}