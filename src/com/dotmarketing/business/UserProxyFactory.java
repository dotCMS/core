package com.dotmarketing.business;

import java.util.HashMap;
import java.util.List;

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;

public abstract class UserProxyFactory {

	public abstract UserProxy getUserProxy(String userId) throws DotRuntimeException, DotHibernateException;
	
	public abstract UserProxy getUserProxyByLongLiveCookie(String dotCMSID) throws DotRuntimeException;
	
	public abstract void saveUserProxy(UserProxy userProxy) throws DotRuntimeException, DotDataException;

	/**
     * This method return all the possible title set by the users.
     * This method will ALWAYS hit the DB
     * @return List<String> of titles.
     */
    public abstract List<String> findUsersTitle() throws DotDataException;
    
    /**
     * This method return a paginated list of the info of user and user proxy that have a firstname, lastname, user proxy title, groups and roles.
     * This method will ALWAYS Hit DB
     * @param firstName compare string
     * @param lastName compare string
     * @param title compare string
     * @param groups list of groups
     * @param showUserGroups boolean. If true then a list of group names will returned in the result. This value will be automatically set to true if the parameter group list is greater than 0 or the order by parameter is ordered by group name.
     * @param roles list of roles
     * @param showUserRoles boolean. If true then a list of role names will returned in the result. This value will be automatically set to true if the parameter role list is greater than 0 or the order by parameter is ordered by role name.
     * @param orderBy how will be ordered the result
     * @param page page to display
     * @param pageSize number of element to show in the page
     * @return HashMap<String, Object>: The list of users found will be associated to the key 'users'. The list of users proxy found will be associated to the key 'usersProxy'. The total number of items returned is associated to the key 'total'. A list of group names can be found and associated to the key 'groupNames' if a group list is passed in. A list of role names can be found and associated to the key 'roleNames' if a role list is passed in.
     * @throws DotDataException 
     * @deprecated
     */
    public abstract HashMap<String, Object> searchUsersAndUsersProxy(String firstName, String lastName, String title, boolean showUserGroups, List<Role> roles, boolean showUserRoles, String orderBy, int page, int pageSize) throws DotDataException;
	
}
