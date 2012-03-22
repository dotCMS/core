package com.dotmarketing.business.ajax;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

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
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.quartz.job.CascadePermissionsJob;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;

import edu.emory.mathcs.backport.java.util.Collections;

public class RoleAjax {

	public List<Map<String, Object>> getRolesTreeFiltered(boolean onlyUserAssignableRoles, String excludeRoles) throws DotDataException{
		return getRolesTree (onlyUserAssignableRoles, excludeRoles, false);
	}

	public List<Map<String, Object>> getRolesTree (boolean onlyUserAssignableRoles, String excludeRoles, boolean excludeUserRoles) throws DotDataException {

		List<Map<String, Object>> toReturn = new ArrayList<Map<String,Object>>();

		RoleAPI roleAPI = APILocator.getRoleAPI();
		List<Role> rootRoles = roleAPI.findRootRoles();

		String[] rolesIds = null;
		if(UtilMethods.isSet(excludeRoles)){
			rolesIds = excludeRoles.split(",");
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

			if(rolesIds!=null){
				for(String roleId: rolesIds){
					if(r.getId().equals(roleId.trim())){
						continue;
					}
					List<String> rolesChildren =r.getRoleChildren();
					List<String> rolesChildrenToSet =new ArrayList <String>();
					if(rolesChildren!=null){
						for(String role :rolesChildren){
							rolesChildrenToSet.add(role);
						}
					}
                    if(rolesChildren!= null && rolesChildren.size()>0){
						for(String roleChild:rolesChildren){
							if(roleChild.equals(roleId.trim())){
								rolesChildrenToSet.remove(roleChild);
							}
						}
						r.setRoleChildren(rolesChildrenToSet);
                    }
				}
			}

			Map<String, Object> roleMap = constructRoleMap(r, onlyUserAssignableRoles);
			toReturn.add(roleMap);

		}
		return toReturn;

	}

	private Map<String, Object> constructRoleMap(Role role, boolean onlyUserAssignableRoles) throws DotDataException {

		RoleAPI roleAPI = APILocator.getRoleAPI();
		Map<String, Object> roleMap = new HashMap<String, Object>();
        if(role!=null){
        	 roleMap = role.toMap();
        }


		List<Map<String, Object>> children = new ArrayList<Map<String,Object>>();

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

				Map<String, Object> childMap = constructRoleMap(childRole, onlyUserAssignableRoles);
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

		RoleAPI roleAPI = APILocator.getRoleAPI();

		Map<String, List<Map<String, Object>>> mapOfUserRoles = new HashMap<String, List<Map<String,Object>>>();
		Set<User> userSet = new HashSet<User>();
		for(String roleId : roleIds) {
			List<User> userList = roleAPI.findUsersForRole(roleId);
			List<Map<String, Object>> userMaps = new ArrayList<Map<String, Object>>();
			if(userList != null)
				for(User u : userList) {
					if(!userSet.contains(u)) {
						userMaps.add(u.toMap());
						userSet.add(u);
					}
				}
			mapOfUserRoles.put(roleId, userMaps);
		}
		return mapOfUserRoles;
	}

	public void removeUsersFromRole(String[] userIds, String roleId) throws DotDataException, NoSuchUserException, DotRuntimeException, PortalException, SystemException, DotSecurityException {

		WebContext ctx = WebContextFactory.get();
		RoleAPI roleAPI = APILocator.getRoleAPI();
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		HttpServletRequest request = ctx.getHttpServletRequest();
		UserAPI uAPI = APILocator.getUserAPI();

		Role role = roleAPI.loadRoleById(roleId);

		for(String userId : userIds) {
			User user = uAPI.loadUserById(userId, uWebAPI.getLoggedInUser(request), !uWebAPI.isLoggedToBackend(request));
			roleAPI.removeRoleFromUser(role, user);
		}
	}

	public Map<String, Object> addUserToRole(String userId, String roleId) throws DotDataException, DotRuntimeException, PortalException, SystemException, DotSecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		WebContext ctx = WebContextFactory.get();
		RoleAPI roleAPI = APILocator.getRoleAPI();
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		HttpServletRequest request = ctx.getHttpServletRequest();
		UserAPI uAPI = APILocator.getUserAPI();

		Role role = roleAPI.loadRoleById(roleId);

		User user = uAPI.loadUserById(userId, uWebAPI.getLoggedInUser(request), !uWebAPI.isLoggedToBackend(request));
		roleAPI.addRoleToUser(role, user);

		return user.toMap();

	}


	public Map<String, Object> addNewRole (String roleName, String roleKey, String parentRoleId, boolean canEditUsers, boolean canEditPermissions,
			boolean canEditLayouts,	String description) throws DotDataException  {
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

		return roleAPI.save(role).toMap();

	}

	public Map<String, Object> updateRole (String roleId, String roleName, String roleKey, String parentRoleId, boolean canEditUsers, boolean canEditPermissions,
			boolean canEditLayouts,	String description) throws DotDataException {
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

		return roleAPI.save(role).toMap();

	}

	public void deleteRole (String roleId) throws DotDataException, DotStateException, DotSecurityException {
		RoleAPI roleAPI = APILocator.getRoleAPI();

		Role role = roleAPI.loadRoleById(roleId);
		roleAPI.delete(role);

	}

	public void lockRole (String roleId) throws DotDataException {
		RoleAPI roleAPI = APILocator.getRoleAPI();
		Role role = roleAPI.loadRoleById(roleId);
		roleAPI.lock(role);

	}

	public void unlockRole (String roleId) throws DotDataException {
		RoleAPI roleAPI = APILocator.getRoleAPI();

		Role role = roleAPI.loadRoleById(roleId);
		roleAPI.unLock(role);

	}

	public List<Map<String, Object>> getAllLayouts() throws DotDataException, LanguageException, DotRuntimeException, PortalException, SystemException {

		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();

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
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();

		LayoutAPI layoutAPI = APILocator.getLayoutAPI();
		RoleAPI roleAPI = APILocator.getRoleAPI();
		Role role = roleAPI.loadRoleById(roleId);

		List<Layout> layouts = layoutAPI.loadLayoutsForRole(role);
		for(Layout l : layouts) {
			list.add(l.toMap());
		}

		return list;

	}

	public void saveRoleLayouts(String roleId, String[] layoutIds) throws DotDataException {

		LayoutAPI layoutAPI = APILocator.getLayoutAPI();
		RoleAPI roleAPI = APILocator.getRoleAPI();
		Role role = roleAPI.loadRoleById(roleId);

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
	}

	/**
	 * Retrieves the info { title, id } of all portlets that can be added to layouts
	 * @return
	 * @throws SystemException
	 * @throws LanguageException
	 * @throws DotRuntimeException
	 * @throws PortalException
	 */
	public List<Map<String, Object>> getAllAvailablePortletInfoList() throws SystemException, LanguageException, DotRuntimeException, PortalException {

		PortletAPI portletAPI = APILocator.getPortletAPI();
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		List<Map<String, Object>> listOfPortletsInfo = new ArrayList<Map<String,Object>>();

		List<Portlet> portlets = portletAPI.findAllPortlets();
		for(Portlet p: portlets) {
			if(portletAPI.canAddPortletToLayout(p)) {
				Map<String, Object> portletMap = new HashMap<String, Object>();
				String portletTitle = LanguageUtil.get(uWebAPI.getLoggedInUser(request),"javax.portlet.title." + p.getPortletId());
				portletMap.put("title", portletTitle);
				portletMap.put("id", p.getPortletId());
				listOfPortletsInfo.add(portletMap);
			}
		}
		Collections.sort(listOfPortletsInfo, new Comparator<Map<String, Object>>() {
			public int compare(Map<String, Object> o1, Map<String, Object> o2) {
				return ((String)o1.get("title")).compareTo(((String)o2.get("title")));
			}
		});

		return listOfPortletsInfo;
	}

	public Map<String, Object> addNewLayout(String layoutName, String layoutDescription, int order, List<String> portletIds) throws DotDataException, LanguageException, DotRuntimeException, PortalException, SystemException {

		LayoutAPI layoutAPI = APILocator.getLayoutAPI();
		Layout newLayout = new Layout();
		newLayout.setName(layoutName);
		newLayout.setDescription(layoutDescription);
		newLayout.setTabOrder(order);
		layoutAPI.saveLayout(newLayout);

		layoutAPI.setPortletIdsToLayout(newLayout, portletIds);

		Map<String, Object> layoutMap =  newLayout.toMap();
		layoutMap.put("portletTitles", getPorletTitlesFromLayout(newLayout));
		return layoutMap;

	}


	public void updateLayout(String layoutId, String layoutName, String layoutDescription, int order, List<String> portletIds) throws DotDataException {

		LayoutAPI layoutAPI = APILocator.getLayoutAPI();

		Layout layout = layoutAPI.findLayout(layoutId);
		layout.setName(layoutName);
		layout.setTabOrder(order);
		layout.setDescription(layoutDescription);
		layoutAPI.saveLayout(layout);

		layoutAPI.setPortletIdsToLayout(layout, portletIds);

	}
	public void deleteLayout(String layoutId) throws DotDataException {

		LayoutAPI layoutAPI = APILocator.getLayoutAPI();
		Layout layout = layoutAPI.loadLayout(layoutId);
		layoutAPI.removeLayout(layout);

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

		Set<Object> permAssets = new HashSet<Object>();
		HashMap<String, List<Permission>> permByInode = new HashMap<String, List<Permission>>();

		RoleAPI roleAPI = APILocator.getRoleAPI();
		PermissionAPI permAPI = APILocator.getPermissionAPI();
		Host systemHost = hostAPI.findSystemHost(systemUser, false);

		Role role = roleAPI.loadRoleById(roleId);

		List<Permission> perms = permAPI.getPermissionsByRole(role, true, true);

		for(Permission p : perms) {
			List<Permission> permList = permByInode.get(p.getInode());
			if(permList == null) {
				permList = new ArrayList<Permission>();
				permByInode.put(p.getInode(), permList);
			}
			permList.add(p);
			Identifier ident = APILocator.getIdentifierAPI().findFromInode(p.getInode());
			if(ident.getAssetType().equals("folder")) {
				Folder f =  APILocator.getFolderAPI().find(p.getInode(), systemUser, respectFrontendRoles);
				permAssets.add(f);
			} else {
				Host h = hostAPI.find(p.getInode(), systemUser, respectFrontendRoles);
				if(h != null) {
					permAssets.add(h);
				}
			}
		}

		List<Map<String, Object>> hostMaps = new ArrayList<Map<String,Object>>();
		List<Map<String, Object>> folderMaps = new ArrayList<Map<String,Object>>();
		boolean systemHostInList = false;
		for(Object i : permAssets) {
			if(i instanceof Host && ((Host)i).isSystemHost())
				systemHostInList = true;
			Map<String, Object> assetMap = i instanceof Host?((Host)i).getMap():((Inode)i).getMap();
			String assetId = i instanceof Host?((Host)i).getIdentifier():((Inode)i).getInode();
			List<Map<String, Object>> permissionsList = new ArrayList<Map<String,Object>>();
			for(Permission p: permByInode.get(assetId)) {
				permissionsList.add(p.getMap());
			}
			assetMap.put("permissions", permissionsList);
			if(i instanceof Host) {
				assetMap.put("type", "host");
				hostMaps.add(assetMap);
			} else {
				Folder f = (Folder) i;
				Identifier id = APILocator.getIdentifierAPI().find(f);
				String hostId = f.getHostId();
				Host h = hostAPI.find(hostId, systemUser, false);
				assetMap.put("fullPath", h.getHostname() + ":" + id.getParentPath() + f.getName());
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
		List<Map<String, Object>> toReturn = new ArrayList<Map<String,Object>>();
		toReturn.addAll(hostMaps);
		toReturn.addAll(folderMaps);
		return toReturn;
	}

	public void saveRolePermission(String roleId, String folderHostId, Map<String, String> permissions, boolean cascade) throws DotDataException, DotSecurityException, PortalException, SystemException {

		Logger.info(this, "Applying role permissions for role " + roleId + " and folder/host id " + folderHostId);

		UserAPI userAPI = APILocator.getUserAPI();
		HostAPI hostAPI = APILocator.getHostAPI();
		FolderAPI folderAPI = APILocator.getFolderAPI();

		HibernateUtil.startTransaction();
		//Retrieving the current user
		User systemUser = userAPI.getSystemUser();
		boolean respectFrontendRoles = false;

		PermissionAPI permissionAPI = APILocator.getPermissionAPI();
		Host host = hostAPI.find(folderHostId, systemUser, false);
		Folder folder = null;
		if(host == null) {
			folder =APILocator.getFolderAPI().find(folderHostId, APILocator.getUserAPI().getSystemUser(), false);
		}
		Permissionable permissionable = host == null?folder:host;
		List<Permission> permissionsToSave = new ArrayList<Permission>();

		if(APILocator.getPermissionAPI().isInheritingPermissions(permissionable)){
			Permissionable parentPermissionable = permissionAPI.findParentPermissionable(permissionable);
			permissionAPI.permissionIndividually(parentPermissionable, permissionable, systemUser, respectFrontendRoles);
		}

		if(permissions.get("individual") != null) {
			int permission = Integer.parseInt(permissions.get("individual"));
			permissionsToSave.add(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, permissionable.getPermissionId(), roleId, permission, true));
		}
		if(permissions.get("hosts") != null) {
			int permission = Integer.parseInt(permissions.get("hosts"));
			permissionsToSave.add(new Permission(Host.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
		}
		if(permissions.get("folders") != null) {
			int permission = Integer.parseInt(permissions.get("folders"));
			permissionsToSave.add(new Permission(Folder.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
		}
		if(permissions.get("containers") != null) {
			int permission = Integer.parseInt(permissions.get("containers"));
			permissionsToSave.add(new Permission(Container.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
		}
		if(permissions.get("templates") != null) {
			int permission = Integer.parseInt(permissions.get("templates"));
			permissionsToSave.add(new Permission(Template.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
		}
		if(permissions.get("files") != null) {
			int permission = Integer.parseInt(permissions.get("files"));
			permissionsToSave.add(new Permission(File.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
		}
		if(permissions.get("links") != null) {
			int permission = Integer.parseInt(permissions.get("links"));
			permissionsToSave.add(new Permission(Link.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
		}
		if(permissions.get("content") != null) {
			int permission = Integer.parseInt(permissions.get("content"));
			permissionsToSave.add(new Permission(Contentlet.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
		}
		// DOTCMS - 3755
		if(permissions.get("pages") != null) {
			int permission = Integer.parseInt(permissions.get("pages"));
			permissionsToSave.add(new Permission(HTMLPage.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
		}
		if(permissions.get("structures") != null) {
			int permission = Integer.parseInt(permissions.get("structures"));
			permissionsToSave.add(new Permission(Structure.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
		}
		if(permissions.get("categories") != null) {
			int permission = Integer.parseInt(permissions.get("categories"));
			permissionsToSave.add(new Permission(Category.class.getCanonicalName(), permissionable.getPermissionId(), roleId, permission, true));
		}


		if(permissionsToSave.size() > 0) {
			permissionAPI.assignPermissions(permissionsToSave, permissionable, systemUser, respectFrontendRoles);
		}

		if(cascade && permissionable.isParentPermissionable()) {
			Logger.info(this, "Cascading permissions for role " + roleId + " and folder/host id " + folderHostId);
			Role role = APILocator.getRoleAPI().loadRoleById(roleId);
			CascadePermissionsJob.triggerJobImmediately(permissionable, role);
			Logger.info(this, "Done cascading permissions for role " + roleId + " and folder/host id " + folderHostId);
		}

		HibernateUtil.commitTransaction();

		Logger.info(this, "Done applying role permissions for role " + roleId + " and folder/host id " + folderHostId);


	}

	public List<Map<String, Object>> getCurrentCascadePermissionsJobs () throws DotDataException, DotSecurityException {
		HostAPI hostAPI = APILocator.getHostAPI();
		FolderAPI folderAPI = APILocator.getFolderAPI();
		RoleAPI roleAPI = APILocator.getRoleAPI();
		List<ScheduledTask> tasks = CascadePermissionsJob.getCurrentScheduledJobs();
		List<Map<String, Object>> scheduled = new ArrayList<Map<String, Object>>();
		for (ScheduledTask task : tasks) {
			Map<String, Object> taskMap = new HashMap<String, Object>();
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

		RoleAPI roleAPI = APILocator.getRoleAPI();
		return roleAPI.loadRoleById(roleId).toMap();

	}

	public Map<String, Object> getUserRole(String userId) throws DotDataException, DotSecurityException, PortalException, SystemException {

		Map<String, Object> toReturn = new HashMap<String, Object>();
		if(UtilMethods.isSet(userId)){
			UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
			WebContext ctx = WebContextFactory.get();
			HttpServletRequest request = ctx.getHttpServletRequest();
			UserAPI userAPI = APILocator.getUserAPI();

			//Retrieving the current user
			User user = userWebAPI.getLoggedInUser(request);
			boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(request);

			User userForRole = userAPI.loadUserById(userId, user, respectFrontendRoles);

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
		List<String> portletTitles = new ArrayList<String>();
		if(portletIds != null) {
			for(String id: portletIds) {
				String portletTitle = LanguageUtil.get(uWebAPI.getLoggedInUser(request),"javax.portlet.title." + id);
				portletTitles.add(portletTitle);
			}
		}

		return portletTitles;
	}

	public Map<String, Object>  isPermissionableInheriting(String assetId) throws DotDataException, DotRuntimeException, PortalException, SystemException, DotSecurityException{

		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		Map<String, Object> ret =  new HashMap<String, Object>();
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
			String assetType ="";
			dc.setSQL("select asset_type as type from identifier where id = ?");
			dc.addParam(assetId);
			ArrayList idResults = dc.loadResults();
			if(idResults.size()>0){
				 assetType = (String)((Map)idResults.get(0)).get("type");

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




}
