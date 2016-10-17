package com.dotmarketing.startup.runalways;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.UUIDGenerator;

/**
 * This task updates the page detail of the structures where there is no url map pattern.
 * Issue https://my.dotcms.com/ticket/dotcms-55/ item #3
 * 
 * @author Erick Gonzalez
 * @version 1.0
 * @since 7-8-2015
 *
 */

public class Task00010CheckAnonymousUser implements StartupTask{
	
	private static final String SQL_SELECT_CMS_ROLE = "SELECT id, parent from cms_role where role_key = ?";
	private static final String SQL_SELECT_USERS_CMS_ROLES = "SELECT id, role_id from users_cms_roles where role_id = ?";
	private static final String SQL_INSERT_CMS_ROLE = "INSERT INTO cms_role (id, role_name, description, role_key, db_fqn, parent, edit_permissions, " +
		"edit_users, edit_layouts, locked, system) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String SQL_INSERT_USERS_CMS_ROLES = "INSERT INTO users_cms_roles (id, user_id, role_id) values (?,?,?)";

	@Override
	public boolean forceRun() {
		return true;
	}

	@Override
	public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        // Check if the anonymous user anonymous exists
        DotConnect dc = new DotConnect();
        dc.setSQL(SQL_SELECT_CMS_ROLE);
        dc.addParam("anonymous");
        ArrayList<Map<String, String>> cmsRole = dc.loadResults();
        
      //If doesn't exists, creates it and also creates the reference in users_cms_role 
        if (cmsRole == null || cmsRole.isEmpty()) {
        	dc = new DotConnect();
        	dc.setSQL(SQL_SELECT_CMS_ROLE);
        	dc.addParam("Users");
        	ArrayList<Map<String,String>> cmsRole_Users = dc.loadResults();
        	if (cmsRole_Users != null && !cmsRole_Users.isEmpty()) {
        		String id_users_cms_role=null;
                for(Map<String, String> role : cmsRole_Users){
                    id_users_cms_role = role.get("id");
                }
                dc = new DotConnect();
                dc.setSQL(SQL_INSERT_CMS_ROLE);
                String newRoleId = UUIDGenerator.generateUuid();
                dc.addParam(newRoleId);
                dc.addParam("anonymous user anonymous");
                dc.addParam("");
                dc.addParam("anonymous");
                dc.addParam(id_users_cms_role + " --> " + newRoleId);
                dc.addParam(id_users_cms_role);
                dc.addParam(true);
                dc.addParam(false);
                dc.addParam(true);
                dc.addParam(false);
                dc.addParam(true);
                dc.loadResult();
                insertUsersCMSRoles(newRoleId,id_users_cms_role);
        	}
        }
        
	
	}

	private void insertUsersCMSRoles(String id_anonymous_cms_role, String parent_anonymous_cms_role) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(SQL_INSERT_USERS_CMS_ROLES);
		dc.addParam(parent_anonymous_cms_role);
		dc.addParam("anonymous");
		dc.addParam(id_anonymous_cms_role);
		dc.loadResult();
	}

}
