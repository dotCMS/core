package com.dotmarketing.business.ajax;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.RoleNameException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.user.ajax.UserAjax;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.quartz.job.CascadePermissionsJob;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import io.vavr.Lazy;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dotmarketing.business.ajax.DwrUtil.getLoggedInUser;
import static com.dotmarketing.business.ajax.DwrUtil.validateRolesPortletPermissions;
import static com.dotmarketing.util.PortletID.LANGUAGES;

/**
 * This class exposes Role and Portlet-related information to the DWR framework. This class is used
 * by several Dojo-based portlets in the dotCMS backend, and will be progressively migrated to the
 * respective REST Endpoint classes in the near future.
 *
 * @author root
 * @since Mar 22nd, 2012
 */
public class RoleAjax {

	private final SystemEventsAPI systemEventsAPI;
	private final PortletAPI portletAPI;
	private final UserWebAPI userWebAPI;

	private static final Lazy<Boolean> HIDE_OLD_LANGUAGES_PORTLET =
			Lazy.of(() -> Config.getBooleanProperty("FEATURE_FLAG_LOCALES_HIDE_OLD_LANGUAGES_PORTLET", true));

    private static final ObjectMapper mapper = DotObjectMapperProvider.getInstance()
            .getDefaultObjectMapper();
    	
	public RoleAjax(){
		this(APILocator.getSystemEventsAPI(), APILocator.getPortletAPI(), WebAPILocator.getUserWebAPI());
	}

	@VisibleForTesting
	protected RoleAjax(final SystemEventsAPI systemEventsAPI, final PortletAPI portletAPI, final UserWebAPI userWebAPI) {
		this.systemEventsAPI = systemEventsAPI;
		this.portletAPI = portletAPI;
		this.userWebAPI = userWebAPI;
    }
	
	public List<Map<String, Object>> getRolesTreeFiltered(boolean onlyUserAssignableRoles, String excludeRoles) throws DotDataException{
		return getRolesTree (onlyUserAssignableRoles, excludeRoles, false);
	}

	public List<Map<String, Object>> getRolesTree (boolean onlyUserAssignableRoles, String excludeRoles, boolean excludeUserRoles) throws DotDataException {

		List<Map<String, Object>> toReturn = new ArrayList<>();

		RoleAPI roleAPI = APILocator.getRoleAPI();
		List<Role> rootRoles = roleAPI.findRootRoles();

		List<String> excludeRolesArray = null;
		if(UtilMethods.isSet(excludeRoles)){
			excludeRoles = excludeRoles +","+getWorkflowRolesId();
			excludeRolesArray = Arrays.asList(excludeRoles.split(","));
		} else {
			excludeRolesArray = Arrays.asList(getWorkflowRolesId().split(","));
		}

		for(Role r : rootRoles) {

			if(onlyUserAssignableRoles) {

				//If the role has no children and is not user assignable then we don't include it
				if(!r.isEditUsers() && (r.getRoleChildren() == null || r.getRoleChildren().size() == 0))
					continue;
				//Special case the users roles branch should be entirely hidden
				if(r.getRoleKey() != null && r.getRoleKey().equals(RoleAPI.USERS_ROOT_ROLE_KEY))
					continue;
			}

			if(excludeUserRoles) {
				if(r.getRoleKey() != null && r.getRoleKey().equals(RoleAPI.USERS_ROOT_ROLE_KEY))
					continue;
			}

			Map<String, Object> roleMap = constructRoleMap(r, excludeRolesArray, onlyUserAssignableRoles);
			toReturn.add(roleMap);

		}
		return toReturn;

	}

	private Map<String, Object> constructRoleMap(Role role, List<String> excludeRoles, boolean onlyUserAssignableRoles) throws DotDataException {

		RoleAPI roleAPI = APILocator.getRoleAPI();
		Map<String, Object> roleMap = new HashMap<>();
        if(role!=null){
        	 roleMap = role.toMap();
        }


		List<Map<String, Object>> children = new ArrayList<>();

		if(role!=null && role.getRoleChildren() != null) {
			for(String id : role.getRoleChildren()) {
				Role childRole = roleAPI.loadRoleById(id);

				if(onlyUserAssignableRoles) {
					//If the role has no children and is not user assignable then we don't include it
					if(!childRole.isEditUsers() && (childRole.getRoleChildren() == null || childRole.getRoleChildren().size() == 0))
						continue;

					//Special case the users roles branch should be entirely hidden
					if(childRole.getRoleKey() != null && childRole.getRoleKey().equals(RoleAPI.USERS_ROOT_ROLE_KEY))
						continue;
				}

				// Exclude roles in the excludeRoles list
				if(excludeRoles.contains(id)) {
					continue;
				}

				Map<String, Object> childMap = constructRoleMap(childRole, excludeRoles, onlyUserAssignableRoles);
				children.add(childMap);
			}
		}

		Collections.sort(children, new Comparator<Map<String, Object>> () {

			public int compare(Map<String, Object> o1, Map<String, Object> o2) {
				return ((String)o1.get("name")).compareTo((String)o2.get("name"));
			}

		});
		roleMap.put("children", children);

		return roleMap;

	}

	//Retrieves a list of roles mapped to the given list of roles
	public Map<String, List<Map<String, Object>>> getUsersByRole(String[] roleIds) throws NoSuchUserException, DotDataException, DotSecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {

		final RoleAPI roleAPI = APILocator.getRoleAPI();

		final Map<String, List<Map<String, Object>>> mapOfUserRoles = new HashMap<>();
		final Set<User> userSet = new HashSet<>();
		for(final String roleId : roleIds) {
			final List<User> userList = roleAPI.findUsersForRole(roleId);
			final List<Map<String, Object>> userMaps = new ArrayList<>();
			if(userList != null)
				for(final User user : userList) {
					if (!userSet.contains(user)) {
                        final Map<String, Object> userMap = getUserMap(user);
                        userMaps.add(userMap);
                        userSet.add(user);
                    }
				}
			mapOfUserRoles.put(roleId, userMaps);
		}
		return mapOfUserRoles;
	}

    /**
     * Returns a map with the user fields, compatible with dojo data store
     * @param user
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    private Map<String, Object> getUserMap(final User user)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        final Map<String, Object> userMap = user.toMap();
        //Additional info is sent as a json object to avoid parsing errors with dojo data store
        final Map<String, String> additionalInfo = (Map<String, String>) userMap
                .get("additionalInfo");
        try {
            userMap.put("additionalInfo",
                    additionalInfo != null ? mapper
                            .writeValueAsString(additionalInfo)
                            : "{}");
        } catch (JsonProcessingException e) {
            Logger.warn(this,
                    "Error generating JSON object for additional user's info. UserId: "
                            + user.getUserId());
            userMap.put("additionalInfo", "{}");
        }
        return userMap;
    }

    /**
	 * Removes a user or set of users from a specific role. This method is
	 * called from the <i>Roles & Tabs</i> page.
	 * 
	 * @param userIds
	 *            - A String array containing the IDs of the users that will be
	 *            removed from the selected role
	 * @param roleId
	 *            - The ID of the role.
	 * @throws DotDataException
	 *             - An error occurred while interacting with the database.
	 * @throws NoSuchUserException
	 *             - A specific user ID does not exist in the database.
	 * @throws DotRuntimeException
	 *             - An serious error has occurred.
	 * @throws PortalException
	 * @throws SystemException
	 * @throws DotSecurityException
	 *             - The current user does not have permission to perform this
	 *             action.
	 */
	public void removeUsersFromRole(String[] userIds, String roleId) throws DotDataException, NoSuchUserException, DotRuntimeException, PortalException, SystemException, DotSecurityException {

		//Validate if this logged in user has the required permissions to access the roles portlet
		validateRolesPortletPermissions(getLoggedInUser());

		WebContext ctx = WebContextFactory.get();
		RoleAPI roleAPI = APILocator.getRoleAPI();
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		HttpServletRequest request = ctx.getHttpServletRequest();
		UserAPI uAPI = APILocator.getUserAPI();
		Role role = roleAPI.loadRoleById(roleId);
		User modUser = getAdminUser();
		String modUserID = modUser != null ? modUser.getUserId() : "";
		String roleName = role != null ? role.getName() : "";
		for (String userId : userIds) {
			User user = uAPI.loadUserById(userId, uWebAPI.getLoggedInUser(request), !uWebAPI.isLoggedToBackend(request));
			String date = DateUtil.getCurrentDate();
			String userID = user != null ? user.getUserId() : "";
			ActivityLogger.logInfo(getClass(), "Removing Role: " + roleName
					+ " to User: " + userID, "Date: " + date + "; " + "User:"
					+ modUserID);
			AdminLogger.log(getClass(), "Removing Role: " + roleName
					+ " to User: " + userID, "Date: " + date + "; " + "User:"
					+ modUserID);
			roleAPI.removeRoleFromUser(role, user);
		}
	}

	public Map<String, Object> addUserToRole(String userId, String roleId) throws DotDataException, DotRuntimeException, PortalException, SystemException, DotSecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {

		//Validate if this logged in user has the required permissions to access the roles portlet
		validateRolesPortletPermissions(getLoggedInUser());

		WebContext ctx = WebContextFactory.get();
		RoleAPI roleAPI = APILocator.getRoleAPI();
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		HttpServletRequest request = ctx.getHttpServletRequest();
		UserAPI uAPI = APILocator.getUserAPI();

		Role role = roleAPI.loadRoleById(roleId);
		User user = uAPI.loadUserById(userId, uWebAPI.getLoggedInUser(request), !uWebAPI.isLoggedToBackend(request));

		User modUser = getAdminUser();
		String date = DateUtil.getCurrentDate();
		ActivityLogger.logInfo(getClass(), "Adding Role: " +role.getName() + " to User: " + user.getUserId() , "Date: " + date + "; "+ "User:" + modUser.getUserId());
		AdminLogger.log(getClass(), "Adding Role: " +role.getName() + " to User: " + user.getUserId() , "Date: " + date + "; "+ "User:" + modUser.getUserId());

		String error = "";
		try{
			roleAPI.addRoleToUser(role, user);
		}catch(DotStateException dse){
			error = LanguageUtil.format(request.getLocale(), "can_not_grant_users_check_rights", new String[]{role.getName()},false);
			ActivityLogger.logInfo(getClass(), "Error Adding Role: " +role.getName() + " to User: " + user.getUserId() , "Date: " + date + "; "+ "User:" + modUser.getUserId());
			AdminLogger.log(getClass(), "Error Adding Role: " +role.getName() + " to User: " + user.getUserId() , "Date: " + date + "; "+ "User:" + modUser.getUserId());
		}

		ActivityLogger.logInfo(getClass(), "Role " + role.getName() + " Added to User: "  + user.getUserId(), "Date: " + date + "; "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );
		AdminLogger.log(getClass(), "Role " + role.getName() + " Added to User: "  + user.getUserId(), "Date: " + date + "; "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );

		Map<String, Object> result = new HashMap<>();
		result.put("error", error);
		result.put("user", getUserMap(user));
		return result;
	}


	public Map<String, Object> addNewRole (String roleName, String roleKey, String parentRoleId, boolean canEditUsers, boolean canEditPermissions,
			boolean canEditLayouts,	String description) throws DotDataException, DotRuntimeException, PortalException, SystemException, DotSecurityException  {
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		User user = getAdminUser();
		RoleAPI roleAPI = APILocator.getRoleAPI();

		Role role = new Role();
		role.setName(roleName);
		role.setRoleKey(roleKey);
		role.setEditUsers(canEditUsers);
		role.setEditPermissions(canEditPermissions);
		role.setEditLayouts(canEditLayouts);
		role.setDescription(description);

		if(parentRoleId != null) {
			Role parentRole = roleAPI.loadRoleById(parentRoleId);
			role.setParent(parentRole.getId());
		}


		String date = DateUtil.getCurrentDate();

		ActivityLogger.logInfo(getClass(), "Adding Role", "Date: " + date + "; "+ "User:" + user.getUserId());
		AdminLogger.log(getClass(), "Adding Role", "Date: " + date + "; "+ "User:" + user.getUserId());

		try {
			role = roleAPI.save(role);
		}  catch(RoleNameException e) {
			ActivityLogger.logInfo(getClass(), "Error Adding Role. Invalid Name", "Date: " + date + ";  "+ "User:" + user.getUserId());
			AdminLogger.log(getClass(), "Error Adding Role. Invalid Name", "Date: " + date + ";  "+ "User:" + user.getUserId());
			throw new DotDataException(LanguageUtil.get(uWebAPI.getLoggedInUser(request),"Role-Save-Name-Failed"),"Role-Save-Name-Failed",e);



		} catch(DotDataException | DotStateException e) {
			ActivityLogger.logInfo(getClass(), "Error Adding Role", "Date: " + date + ";  "+ "User:" + user.getUserId());
			AdminLogger.log(getClass(), "Error Adding Role", "Date: " + date + ";  "+ "User:" + user.getUserId());
			throw e;
		}

		ActivityLogger.logInfo(getClass(), "Role Created", "Date: " + date + "; "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );
		AdminLogger.log(getClass(), "Role Created", "Date: " + date + "; "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );

		return role.toMap();

	}

	public Map<String, Object> updateRole (String roleId, String roleName, String roleKey, String parentRoleId, boolean canEditUsers, boolean canEditPermissions,
			boolean canEditLayouts,	String description) throws DotDataException, DotRuntimeException, PortalException, SystemException, DotSecurityException {
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		User user = getAdminUser();
		RoleAPI roleAPI = APILocator.getRoleAPI();

		Role role = roleAPI.loadRoleById(roleId);
		role.setName(roleName);
		role.setRoleKey(roleKey);
		role.setEditUsers(canEditUsers);
		role.setEditPermissions(canEditPermissions);
		role.setEditLayouts(canEditLayouts);
		role.setDescription(description);

		if(parentRoleId != null) {
			Role parentRole = roleAPI.loadRoleById(parentRoleId);
			role.setParent(parentRole.getId());
		} else {
			role.setParent(role.getId());
		}


		String date = DateUtil.getCurrentDate();

		ActivityLogger.logInfo(getClass(), "Modifying Role", "Date: " + date + "; "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );
		AdminLogger.log(getClass(), "Modifying Role", "Date: " + date + "; "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );

		try {
			role = roleAPI.save(role);
		} catch(RoleNameException e) {
			ActivityLogger.logInfo(getClass(), "Error Adding Role. Invalid Name", "Date: " + date + ";  "+ "User:" + user.getUserId());
			AdminLogger.log(getClass(), "Error Adding Role. Invalid Name", "Date: " + date + ";  "+ "User:" + user.getUserId());
            throw new DotDataException(LanguageUtil.get(uWebAPI.getLoggedInUser(request),"Role-Save-Name-Failed"),"Role-Save-Name-Failed",e);

		} catch(DotDataException | DotStateException e) {
			ActivityLogger.logInfo(getClass(), "Error Modifying Role", "Date: " + date + ";  "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );
			AdminLogger.log(getClass(), "Error Modifying Role", "Date: " + date + ";  "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );
			throw e;
		}

		ActivityLogger.logInfo(getClass(), "Role Modified", "Date: " + date + "; "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );
		AdminLogger.log(getClass(), "Role Modified", "Date: " + date + "; "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );

		return role.toMap();

	}



	public boolean deleteRole (String roleId) throws DotDataException, DotStateException, DotSecurityException, SystemException, PortalException {
		RoleAPI roleAPI = APILocator.getRoleAPI();
		Role role = roleAPI.loadRoleById(roleId);
		User user = getAdminUser();
		String date = DateUtil.getCurrentDate();		

		ActivityLogger.logInfo(getClass(), "Deleting Role", "Date: " + date + "; "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );
		AdminLogger.log(getClass(), "Deleting Role", "Date: " + date + "; "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );
		if(role.getRoleChildren() == null || role.getRoleChildren().size() == 0){
			try {			
				roleAPI.delete(role);
				ActivityLogger.logInfo(getClass(), "Role Deleted", "Date: " + date + "; "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );
				AdminLogger.log(getClass(), "Role Deleted", "Date: " + date + "; "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );	
				return true;
			} catch(DotDataException | DotStateException e) {
				ActivityLogger.logInfo(getClass(), "Error Deleting Role", "Date: " + date + ";  "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );
				AdminLogger.log(getClass(), "Error Deleting Role", "Date: " + date + ";  "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );
				throw e;
			}
		}else{
			return false;
		}	
		
	}

	private User getAdminUser() throws PortalException, SystemException, DotSecurityException {
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		User loggedInUser = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
	    // lock down to users with access to Users portlet
		String remoteIp = request.getRemoteHost();
		String userId = "[not logged in]";
		if(loggedInUser!=null && loggedInUser.getUserId()!=null){
			userId = loggedInUser.getUserId();
		}
        if(loggedInUser==null || !APILocator.getPortletAPI().hasUserAdminRights(loggedInUser)) {
        	SecurityLogger.logInfo(UserAjax.class, "unauthorized attempt to call getUserById by user "+ userId+ " from " + remoteIp);
        	throw new DotSecurityException("not authorized");
        }
        return loggedInUser;
	}

	public void lockRole (String roleId) throws DotDataException, PortalException, SystemException, DotSecurityException {
		RoleAPI roleAPI = APILocator.getRoleAPI();
		Role role = roleAPI.loadRoleById(roleId);

		User user = getAdminUser();
		String date = DateUtil.getCurrentDate();

		ActivityLogger.logInfo(getClass(), "Locking Role", "Date: " + date + "; " + "User:" + user.getUserId() + "; RoleID: " + role.getId() );
		AdminLogger.log(getClass(), "Locking Role", "Date: " + date + "; " + "User:" + user.getUserId() + "; RoleID: " + role.getId() );

		try {
			roleAPI.lock(role);
		} catch(DotDataException e) {
			ActivityLogger.logInfo(getClass(), "Error Locking Role", "Date: " + date + ";  "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );
			AdminLogger.log(getClass(), "Error Locking Role", "Date: " + date + ";  "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );
			throw e;
		}

		ActivityLogger.logInfo(getClass(), "Role Locked", "Date: " + date + "; "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );
		AdminLogger.log(getClass(), "Role Locked", "Date: " + date + "; "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );

	}

	public void unlockRole (String roleId) throws DotDataException, PortalException, SystemException, DotSecurityException {
		RoleAPI roleAPI = APILocator.getRoleAPI();

		Role role = roleAPI.loadRoleById(roleId);

		User user = getAdminUser();
		String date = DateUtil.getCurrentDate();

		ActivityLogger.logInfo(getClass(), "Unlocking Role", "Date:" + date + "; "+ "User:" + user.getUserId() + "; RoleID:" + role.getId() );
		AdminLogger.log(getClass(), "Unlocking Role", "Date:" + date + "; "+ "User:" + user.getUserId() + "; RoleID:" + role.getId() );

		try {
			roleAPI.unLock(role);
		} catch(DotDataException e) {
			ActivityLogger.logInfo(getClass(), "Error Unlocking Role", "Date:" + date + ";  "+ "User:" + user.getUserId() + "; RoleID:" + role.getId() );
			AdminLogger.log(getClass(), "Error Unlocking Role", "Date:" + date + ";  "+ "User:" + user.getUserId() + "; RoleID:" + role.getId() );
			throw e;
		}

		ActivityLogger.logInfo(getClass(), "Role Unlocked", "Date:" + date + "; "+ "User:" + user.getUserId() + "; RoleID:" + role.getId() );
		AdminLogger.log(getClass(), "Role Unlocked", "Date:" + date + "; "+ "User:" + user.getUserId() + "; RoleID:" + role.getId() );

	}

	public List<Map<String, Object>> getAllLayouts() throws DotDataException, LanguageException, DotRuntimeException, PortalException, SystemException {

		List<Map<String, Object>> list = new ArrayList<>();

		LayoutAPI layoutAPI = APILocator.getLayoutAPI();

		List<Layout> layouts = layoutAPI.findAllLayouts();
		for(Layout l: layouts) {
			Map<String, Object> layoutMap = l.toMap();
			layoutMap.put("portletTitles", getPorletTitlesFromLayout(l));
			list.add(layoutMap);
		}
		return list;

	}

	public List<Map<String, Object>> loadRoleLayouts(String roleId) throws DotDataException {
		List<Map<String, Object>> list = new ArrayList<>();

		LayoutAPI layoutAPI = APILocator.getLayoutAPI();
		RoleAPI roleAPI = APILocator.getRoleAPI();
		Role role = roleAPI.loadRoleById(roleId);

		List<Layout> layouts = layoutAPI.loadLayoutsForRole(role);
		for(Layout l : layouts) {
			list.add(l.toMap());
		}

		return list;

	}

	public void saveRoleLayouts(String roleId, String[] layoutIds) throws DotDataException, PortalException, SystemException, DotSecurityException {

		//Validate if this logged in user has the required permissions to access the roles portlet
		validateRolesPortletPermissions(getLoggedInUser());

		LayoutAPI layoutAPI = APILocator.getLayoutAPI();
		RoleAPI roleAPI = APILocator.getRoleAPI();
		Role role = roleAPI.loadRoleById(roleId);
		User user = getAdminUser();
		List<Layout> layouts = layoutAPI.loadLayoutsForRole(role);

		//Looking for removed layouts
		for(Layout l : layouts) {
			boolean found = false;
			for(String changedLayout: layoutIds) {
				if(changedLayout.equals(l.getId())) {
					found = true;
					break;
				}
			}
			if(!found) {
				roleAPI.removeLayoutFromRole(l, role);
			}
		}

		//Looking for added layouts
		for(String changedLayout : layoutIds) {
			boolean found = false;
			for(Layout l : layouts) {
				if(changedLayout.equals(l.getId())) {
					found = true;
					break;
				}
			}
			Layout layout = layoutAPI.loadLayout(changedLayout);
			if(!found) {
				roleAPI.addLayoutToRole(layout, role);
			}
		}
		
		//Send a websocket event to notificate a layout change  
		systemEventsAPI.pushAsync(SystemEventType.UPDATE_PORTLET_LAYOUTS, new Payload());
				
	}

	/**
	 * Retrieves the title and ID of all portlets that can be added to the main menu -- i.e.,
	 * layouts --  in the dotCMS backend
	 *
	 * @return A list of maps, each containing the title and ID of a portlet that can be added to
	 * the main menu.
	 *
	 * @throws SystemException   An error occurred when retrieving all Portlets from the database.
	 * @throws LanguageException An error occurred when retrieving the localized title of a
	 *                           Portlet.
	 */
	@SuppressWarnings("unused")
	public List<Map<String, Object>> getAllAvailablePortletInfoList() throws SystemException,
			LanguageException {
		final HttpServletRequest request = DwrUtil.getHttpServletRequest();
		final List<Map<String, Object>> listOfPortletsInfo = new ArrayList<>();
		final Collection<Portlet> portlets = this.portletAPI.findAllPortlets();
		for (final Portlet portlet : portlets) {
			if (LANGUAGES.name().equalsIgnoreCase(portlet.getPortletId())
					&& Boolean.TRUE.equals(HIDE_OLD_LANGUAGES_PORTLET.get())) {
				continue;
			}
			if (this.portletAPI.canAddPortletToLayout(portlet)) {
				final String portletTitle = LanguageUtil.get(this.userWebAPI.getLoggedInUser(request),
								"com.dotcms.repackage.javax.portlet.title." + portlet.getPortletId());
				listOfPortletsInfo.add(Map.of(
						"title", portletTitle,
						"id", portlet.getPortletId()
				));
			}
		}
		listOfPortletsInfo.sort(Comparator.comparing(o -> ((String) o.get("title")).toLowerCase()));
		return listOfPortletsInfo;
	}

	public Map<String, Object> addNewLayout(String layoutName, String layoutDescription, int order, List<String> portletIds) throws DotDataException, LanguageException, DotRuntimeException, PortalException, SystemException, DotSecurityException {
		User user = getAdminUser();
		LayoutAPI layoutAPI = APILocator.getLayoutAPI();
		Layout newLayout = new Layout();
		newLayout.setName(layoutName);
		newLayout.setDescription(layoutDescription);
		newLayout.setTabOrder(order);
		layoutAPI.saveLayout(newLayout);

		layoutAPI.setPortletIdsToLayout(newLayout, portletIds);
		
		//Send a websocket event to notificate a layout change  
		systemEventsAPI.pushAsync(SystemEventType.UPDATE_PORTLET_LAYOUTS, new Payload());
				
		Map<String, Object> layoutMap =  newLayout.toMap();
		layoutMap.put("portletTitles", getPorletTitlesFromLayout(newLayout));
		return layoutMap;
	}


	public void updateLayout(String layoutId, String layoutName, String layoutDescription, int order, List<String> portletIds) throws DotDataException, PortalException, SystemException, DotSecurityException {

		//Validate if this logged in user has the required permissions to access the roles portlet
		validateRolesPortletPermissions(getLoggedInUser());

		User user = getAdminUser();
		LayoutAPI layoutAPI = APILocator.getLayoutAPI();

		Layout layout = layoutAPI.findLayout(layoutId);
		layout.setName(layoutName);
		layout.setTabOrder(order);
		layout.setDescription(layoutDescription);
		layoutAPI.saveLayout(layout);

		layoutAPI.setPortletIdsToLayout(layout, portletIds);
		
		//Send a websocket event to notificate a layout change  
		systemEventsAPI.pushAsync(SystemEventType.UPDATE_PORTLET_LAYOUTS, new Payload());
				
	}
	
	public void deleteLayout(String layoutId) throws DotDataException, PortalException, SystemException, DotSecurityException {

		//Validate if this logged in user has the required permissions to access the roles portlet
		validateRolesPortletPermissions(getLoggedInUser());

		User user = getAdminUser();
		LayoutAPI layoutAPI = APILocator.getLayoutAPI();
		Layout layout = layoutAPI.loadLayout(layoutId);
		layoutAPI.removeLayout(layout);
		
		//Send a websocket event to notificate a layout change  
		systemEventsAPI.pushAsync(SystemEventType.UPDATE_PORTLET_LAYOUTS, new Payload());
	}

	/**
	 * This methods returns a list of folders and hosts and for each folder/host includes a property with permissions
	 * of the role over the asset, it also sorts them by hosts first then folders
	 * @param roleId
	 * @return
	 * @throws DotDataException
	 * @throws SystemException
	 * @throws PortalException
	 * @throws DotRuntimeException
	 */
	public List<Map<String, Object>> getRolePermissions(String roleId) throws DotDataException, DotSecurityException, PortalException, SystemException {

		//Validate if this logged in user has the required permissions to access the roles portlet
		validateRolesPortletPermissions(getLoggedInUser());

		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		UserAPI userAPI = APILocator.getUserAPI();
		HostAPI hostAPI = APILocator.getHostAPI();
		FolderAPI folderAPI = APILocator.getFolderAPI();

		//Retrieving the current user
		User user = userWebAPI.getLoggedInUser(request);
		User systemUser = userAPI.getSystemUser();
		boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(request);

		Set<Object> permAssets = new HashSet<>();
		HashMap<String, List<Permission>> permByInode = new HashMap<>();

		RoleAPI roleAPI = APILocator.getRoleAPI();
		PermissionAPI permAPI = APILocator.getPermissionAPI();
		Host systemHost = hostAPI.findSystemHost(systemUser, false);

		Role role = roleAPI.loadRoleById(roleId);

		List<Permission> perms = permAPI.getPermissionsByRole(role, true, true);

		for(Permission p : perms) {
			List<Permission> permList = permByInode.get(p.getInode());
			if(permList == null) {
				permList = new ArrayList<>();
				permByInode.put(p.getInode(), permList);
			}
			permList.add(p);
			final Folder folder =  APILocator.getFolderAPI().find(p.getInode(), systemUser, respectFrontendRoles);
			if(null != folder && UtilMethods.isSet(folder.getIdentifier())) {

				permAssets.add(folder);
			} else {
				Host h = hostAPI.find(p.getInode(), systemUser, respectFrontendRoles);
				if(h != null) {
					permAssets.add(h);
				}
			}
		}

		List<Map<String, Object>> hostMaps = new ArrayList<>();
		List<Map<String, Object>> folderMaps = new ArrayList<>();
		boolean systemHostInList = false;
		for(Object i : permAssets) {
			if(i instanceof Host && ((Host)i).isSystemHost())
				systemHostInList = true;
			Map<String, Object> assetMap = i instanceof Host?((Host)i).getMap():((Folder)i).getMap();
			String assetId = i instanceof Host?((Host)i).getIdentifier():((Folder)i).getInode();
			List<Map<String, Object>> permissionsList = new ArrayList<>();
			for(Permission p: permByInode.get(assetId)) {
				permissionsList.add(p.getMap());
			}
			assetMap.put("permissions", permissionsList);
			if(i instanceof Host) {
				assetMap.put("type", "host");
				hostMaps.add(assetMap);
			} else {
				final Folder folder = (Folder) i;
				Identifier id = APILocator.getIdentifierAPI().find(folder.getIdentifier());
				String hostId = folder.getHostId();
				Host h = hostAPI.find(hostId, systemUser, false);
				assetMap.put("fullPath", h.getHostname() + ":" + id.getParentPath() + folder.getName());
				folderMaps.add(assetMap);
			}
			boolean permissionToEditPermissions = permAPI.doesUserHavePermission((Permissionable)i, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user, respectFrontendRoles);
			assetMap.put("permissionToEditPermissions", permissionToEditPermissions);
		}

		if(!systemHostInList) {
			Map<String, Object> systemHostMap = systemHost.getMap();
			systemHostMap.put("type", "host");
			boolean permissionToEditPermissions = permAPI.doesUserHavePermission(systemHost, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user, respectFrontendRoles);
			systemHostMap.put("permissionToEditPermissions", permissionToEditPermissions);
			systemHostMap.put("permissions", new ArrayList<Map<String, Object>>());
			hostMaps.add(systemHostMap);
		}
		List<Map<String, Object>> toReturn = new ArrayList<>();
		toReturn.addAll(hostMaps);
		toReturn.addAll(folderMaps);
		return toReturn;
	}

	public void saveRolePermission(String roleId, String folderHostId, Map<String, String> permissions, boolean cascade) throws DotDataException, DotSecurityException, PortalException, SystemException {

		//Validate if this logged in user has the required permissions to access the roles portlet
		validateRolesPortletPermissions(getLoggedInUser());

		Logger.info(this, "Applying role permissions for role " + roleId + " and folder/host id " + folderHostId);

		UserAPI userAPI = APILocator.getUserAPI();
		HostAPI hostAPI = APILocator.getHostAPI();
        RoleAPI roleAPI = APILocator.getRoleAPI();

		HibernateUtil.startTransaction();
		try {
			//Retrieving the current user
			User systemUser = userAPI.getSystemUser();
			boolean respectFrontendRoles = false;

			PermissionAPI permissionAPI = APILocator.getPermissionAPI();
			Host host = hostAPI.find(folderHostId, systemUser, false);
			Folder folder = null;
			if ( host == null ) {
				folder = APILocator.getFolderAPI().find(folderHostId, APILocator.getUserAPI().getSystemUser(), false);
			}
			Permissionable permissionable = host == null ? folder : host;
			List<Permission> permissionsToSave = new ArrayList<>();

			if ( APILocator.getPermissionAPI().isInheritingPermissions(permissionable) ) {
				Permissionable parentPermissionable = permissionAPI.findParentPermissionable(permissionable);
				permissionAPI.permissionIndividuallyByRole(parentPermissionable, permissionable, systemUser, roleAPI.loadRoleById(roleId));
			}

			if ( permissions.get("individual") != null ) {
				int permission = Integer.parseInt(permissions.get("individual"));
				permissionsToSave.add(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, permissionable.getPermissionId(), roleId, permission, true));
			}
			if ( permissions.get("hosts") != null ) {
				int permission = Integer.parseInt(permissions.get("hosts"));
				permissionsToSave.add(new Permission(Host.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
			}
			if ( permissions.get("folders") != null ) {
				int permission = Integer.parseInt(permissions.get("folders"));
				permissionsToSave.add(new Permission(Folder.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
			}
			if ( permissions.get("containers") != null ) {
				int permission = Integer.parseInt(permissions.get("containers"));
				permissionsToSave.add(new Permission(Container.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
			}
			if ( permissions.get("templates") != null ) {
				int permission = Integer.parseInt(permissions.get("templates"));
				permissionsToSave.add(new Permission(Template.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
			}
			if ( permissions.get("templateLayouts") != null ) {
				int permission = Integer.parseInt(permissions.get("templateLayouts"));
				permissionsToSave.add(new Permission(TemplateLayout.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
			}

			if ( permissions.get("links") != null ) {
				int permission = Integer.parseInt(permissions.get("links"));
				permissionsToSave.add(new Permission(Link.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
			}
			if ( permissions.get("content") != null ) {
				int permission = Integer.parseInt(permissions.get("content"));
				permissionsToSave.add(new Permission(Contentlet.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
			}
			// DOTCMS - 3755
			if ( permissions.get("pages") != null ) {
				int permission = Integer.parseInt(permissions.get("pages"));
				permissionsToSave.add(new Permission(IHTMLPage.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
			}
			if ( permissions.get("structures") != null ) {
				int permission = Integer.parseInt(permissions.get("structures"));
				permissionsToSave.add(new Permission(Structure.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
			}
			if ( permissions.get("categories") != null ) {
				int permission = Integer.parseInt(permissions.get("categories"));
				permissionsToSave.add(new Permission(Category.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
			}
			if ( permissions.get("rules") != null ) {
				int permission = Integer.parseInt(permissions.get("rules"));
				permissionsToSave.add(new Permission(Rule.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
			}


			if ( permissionsToSave.size() > 0 ) {
				// NOTE: Method "assignPermissions" is deprecated in favor of "save", which has subtle functional differences. Please take these differences into consideration if planning to replace this method with the "save"
				permissionAPI.assignPermissions(permissionsToSave, permissionable, systemUser, respectFrontendRoles);
			}

			if ( cascade && permissionable.isParentPermissionable() ) {
				Logger.info(this, "Cascading permissions for role " + roleId + " and folder/host id " + folderHostId);
				Role role = APILocator.getRoleAPI().loadRoleById(roleId);
				CascadePermissionsJob.triggerJobImmediately(permissionable, role);
				Logger.info(this, "Done cascading permissions for role " + roleId + " and folder/host id " + folderHostId);
			}
			
			HibernateUtil.closeAndCommitTransaction();

		} catch (Exception e) {
			Logger.error(this, "Error saving permissions for role " + roleId + " and folder/host id:" + folderHostId, e);
			HibernateUtil.rollbackTransaction();
		} finally {
			HibernateUtil.closeSession();
		}

		Logger.info(this, "Done applying role permissions for role " + roleId + " and folder/host id " + folderHostId);
	}

	public List<Map<String, Object>> getCurrentCascadePermissionsJobs () throws DotDataException, DotSecurityException, PortalException, SystemException {
		//Validate if this logged in user has the required permissions to access the roles portlet
		validateRolesPortletPermissions(getLoggedInUser());

		HostAPI hostAPI = APILocator.getHostAPI();
		FolderAPI folderAPI = APILocator.getFolderAPI();
		RoleAPI roleAPI = APILocator.getRoleAPI();
		List<ScheduledTask> tasks = CascadePermissionsJob.getCurrentScheduledJobs();
		List<Map<String, Object>> scheduled = new ArrayList<>();
		for (ScheduledTask task : tasks) {
			Map<String, Object> taskMap = new HashMap<>();
			Map<String, Object> props = task.getProperties();
			String permissionableId = (String) props.get("permissionableId");
			String roleId = (String) props.get("roleId");
			if(permissionableId == null || roleId == null)
				continue;
			Host host = hostAPI.find(permissionableId, APILocator.getUserAPI().getSystemUser(), false);
			if(host == null) {
				Folder folder = APILocator.getFolderAPI().find(permissionableId, APILocator.getUserAPI().getSystemUser(), false);
				if(folder == null)
					continue;
				taskMap.put("folder", folder.getMap());
				host = hostAPI.findParentHost(folder,APILocator.getUserAPI().getSystemUser(), false);
				taskMap.put("host", host.getMap());
			} else {
				taskMap.put("host", host.getMap());
			}
			Role role = roleAPI.loadRoleById(roleId);
			if(role == null)
				continue;
			taskMap.put("role", role.toMap());
			scheduled.add(taskMap);
		}
		return scheduled;
	}

	public Map<String, Object> getRole(String roleId) throws DotDataException, DotSecurityException, PortalException, SystemException {

		//Validate if this logged in user has the required permissions to access the roles portlet
		validateRolesPortletPermissions(getLoggedInUser());

		RoleAPI roleAPI = APILocator.getRoleAPI();
		return roleAPI.loadRoleById(roleId).toMap();

	}

	public Map<String, Object> getUserRole(String userId) throws DotDataException, DotSecurityException, PortalException, SystemException {

		//Validate if this logged in user has the required permissions to access the roles portlet
		validateRolesPortletPermissions(getLoggedInUser());

		Map<String, Object> toReturn = new HashMap<>();

		if(UtilMethods.isSet(userId)){
			UserAPI userAPI = APILocator.getUserAPI();
			User userForRole = userAPI.loadUserById(userId);
			RoleAPI roleAPI = APILocator.getRoleAPI();
			toReturn = roleAPI.getUserRole(userForRole).toMap();
		}
		return toReturn;

	}

	private List<String> getPorletTitlesFromLayout (Layout l) throws LanguageException, DotRuntimeException, PortalException, SystemException {

		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		List<String> portletIds = l.getPortletIds();
		List<String> portletTitles = new ArrayList<>();
		if(portletIds != null) {
			for(String id: portletIds) {
				String portletTitle = LanguageUtil.get(uWebAPI.getLoggedInUser(request),"com.dotcms.repackage.javax.portlet.title." + id);
				portletTitles.add(portletTitle);
			}
		}

		return portletTitles;
	}

	@CloseDBIfOpened
	public Map<String, Object>  isPermissionableInheriting(String assetId) throws DotDataException, DotRuntimeException, PortalException, SystemException, DotSecurityException{

		//Validate if this logged in user has the required permissions to access the roles portlet
		validateRolesPortletPermissions(getLoggedInUser());

		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		Map<String, Object> ret =  new HashMap<>();
		ret.put("isInheriting", false);


		//Retrieving the current user
		User user = userWebAPI.getLoggedInUser(request);
		boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(request);

        HostAPI hostAPI = APILocator.getHostAPI();

		Permissionable perm = null;

		//Determining the type

		//Host?
		perm = hostAPI.find(assetId, user, respectFrontendRoles);
		if(perm == null) {
			//Content?
			ContentletAPI contAPI = APILocator.getContentletAPI();
			try {
				perm = contAPI.findContentletByIdentifier(assetId, false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, respectFrontendRoles);
			} catch (DotContentletStateException e) {
			}
		}
		if(perm == null) {
			DotConnect dc = new DotConnect();
			dc.setSQL("select asset_type as type from identifier where id = ?");
			dc.addParam(assetId);
			ArrayList idResults = dc.loadResults();
			if(idResults.size()>0){
				String assetType = (String)((Map)idResults.get(0)).get("type");

				 if (Folder.FOLDER_TYPE.equals(assetType)){
					 perm = APILocator.getFolderAPI().find(assetId, user, respectFrontendRoles);
				 } else{
					 dc.setSQL("select inode, type from inode,"+assetType+" asset,identifier where inode.inode = asset.inode and " +
							 "asset.identifier = identifier.id and asset.identifier = ?");
					 dc.addParam(assetId);
					 ArrayList results = dc.loadResults();

					 if(results.size() > 0) {
						 String inode = (String) ((Map)results.get(0)).get("inode");
						 perm = InodeFactory.getInode(inode, Inode.class);
					 }
				 }
			}
		}

		if(perm == null || !UtilMethods.isSet(perm.getPermissionId())) {
			perm = InodeFactory.getInode(assetId, Inode.class);
		}

		if(perm!=null && UtilMethods.isSet(perm.getPermissionId())){
			boolean isInheriting=APILocator.getPermissionAPI().isInheritingPermissions(perm);
			if(isInheriting){
				ret.put("isInheriting", true);
			}
		}

		return ret;

	}

	/**
	 * Get a string comma separated with the workflow special roles. These Workflow roles should not
	 * be displayed in the permission tabs
	 *
	 * @return String of comma separated ID's of the workflow roles
	 */
	private String getWorkflowRolesId() throws DotDataException {

		StringBuilder workflowRolesIds = new StringBuilder();
		try {
			for (Role role : APILocator.getRoleAPI().findWorkflowSpecialRoles()) {
				if (workflowRolesIds.length() > 0) {
					workflowRolesIds.append(",").append(role.getId());
				} else {
					workflowRolesIds.append(role.getId());
				}
			}

		} catch (DotSecurityException e) {
			Logger.error(this, "Error getting workflow roles.", e);

		}
		return workflowRolesIds.toString();
	}

}