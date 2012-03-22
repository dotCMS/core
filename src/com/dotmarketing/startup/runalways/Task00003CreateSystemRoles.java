package com.dotmarketing.startup.runalways;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.util.PropsUtil;

public class Task00003CreateSystemRoles implements StartupTask {

	private final String getSystemRole = "select id, role_name, description, role_key, db_fqn, parent, edit_permissions, edit_users, edit_layouts, " +
		"locked, system from cms_role where role_name = 'System' and id = parent";

	private final String getUsersRole = "select id, role_name, description, role_key, db_fqn, parent, edit_permissions, edit_users, edit_layouts, " +
		"locked, system from cms_role where role_name = 'Users' and id = parent";

	private final String selectSystemRoles = "select id, role_name, description, role_key, db_fqn, parent, edit_permissions, edit_users, edit_layouts, " +
		"locked, system from cms_role where parent = ? and parent <> id";

	private final String insertRole = "insert into cms_role (id, role_name, description, role_key, db_fqn, parent, edit_permissions, " +
		"edit_users, edit_layouts, locked, system) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private final String[] rolesWithUsersLocked = { "LDAP User", "CMS Owner", "CMS Anonymous", "LoggedIn Site User" };

	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		
		DotConnect dc = new DotConnect();

		dc.setSQL(getSystemRole);
		ArrayList<Map<String, String>> results;
		try {
			results = dc.loadResults();
		} catch (DotDataException e) {
			Logger.info(this, "Task not executing seems the roles has not been upgraded yet");
			return;
		}
		String systemRootRoleId = null; 
		if(results.size() == 0) {
			systemRootRoleId = UUIDGenerator.generateUuid();
			dc.setSQL(insertRole);
			dc.addParam(systemRootRoleId);
			dc.addParam("System");
			dc.addParam("System roles root");
			dc.addParam(RoleAPI.SYSTEM_ROOT_ROLE_KEY);
			dc.addParam(systemRootRoleId);
			dc.addParam(systemRootRoleId);
			dc.addParam(false);
			dc.addParam(false);
			dc.addParam(false);
			dc.addParam(false);
			dc.addParam(true);
			dc.loadResult();
		} else {
			systemRootRoleId = results.get(0).get("id");
		}
		
		dc.setSQL(getUsersRole);
		results = dc.loadResults();
		String usersRootRoleId = null; 
		if(results.size() == 0) {
			usersRootRoleId = UUIDGenerator.generateUuid();
			dc.setSQL(insertRole);
			dc.addParam(usersRootRoleId);
			dc.addParam("Users");
			dc.addParam("User Roles root");
			dc.addParam(RoleAPI.USERS_ROOT_ROLE_KEY);
			dc.addParam(usersRootRoleId);
			dc.addParam(usersRootRoleId);
			dc.addParam(false);
			dc.addParam(false);
			dc.addParam(false);
			dc.addParam(false);
			dc.addParam(true);
			dc.loadResult();
		} else {
			usersRootRoleId = results.get(0).get("id");
		}
		
		String[] systemRoles = PropsUtil.getArray(PropsUtil.SYSTEM_ROLES);
		dc.setSQL(selectSystemRoles);
		dc.addParam(systemRootRoleId);
		List<Map<String, String>> currentSystemRoles = dc.loadResults();

		Arrays.sort(rolesWithUsersLocked);
		
		for(String roleName : systemRoles) {
			if(!containsRole(roleName, currentSystemRoles)) {
				
				String newRoleId = UUIDGenerator.generateUuid();
				dc.setSQL(insertRole);
				dc.addParam(newRoleId); 								//id
				dc.addParam(roleName.trim()); 							//role_name
				dc.addParam(roleName.trim()); 										//description
				dc.addParam(roleName.trim());							//key
				dc.addParam(systemRootRoleId + " --> " + newRoleId);	//db_fqn
				dc.addParam(systemRootRoleId);							//parent
				if(roleName.equals("CMS Administrator"))				//edit_permission
					dc.addParam(false);									
				else
					dc.addParam(true);									
				dc.addParam(Arrays.binarySearch(rolesWithUsersLocked, roleName.trim()) > -1?false: true);										//edit users
				dc.addParam(true);										//edit layouts
				dc.addParam(true);										//locked
				dc.addParam(true);										//system
				dc.loadResult();
			}
		}
		
	}

	private boolean containsRole (String roleName, List<Map<String, String>> systemRoles) {
		for(Map<String, String> systemRole : systemRoles) {
			if(systemRole.get("role_name").equals(roleName.trim()))
				return true;
		}
		return false;
	}
	
	public boolean forceRun() {

		return true;
	}

}
