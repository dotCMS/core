package com.dotmarketing.viewtools;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class CASTool implements ViewTool {
	
	public static String LDAP_USER_ROLE = "LDAP User";
	private static RoleAPI roleAPI = APILocator.getRoleAPI();

	@Override
	public void init(Object initData) {
	}

	public boolean isCASUserLoggedIn(User user, HttpServletRequest req) {
		
		List<Role> userRoles = new ArrayList<Role>();
		boolean isLDAPUser = false;
		String userIdFromCAS = "";
		try {
			if(UtilMethods.isSet(user)){
				userIdFromCAS = (String)req.getSession(false).getAttribute("edu.yale.its.tp.cas.client.filter.user");
				userRoles = roleAPI.loadRolesForUser(user.getUserId());

				for(Role r: userRoles){
					if(r.getName().equalsIgnoreCase(LDAP_USER_ROLE)){
						isLDAPUser = true;
						break;
					}
				}
				if(UtilMethods.isSet(userIdFromCAS) && isLDAPUser){
					return true;
				}
			}
		} catch (DotDataException e) {
			Logger.error(CASTool.class,"There was a problem with the logged-in user validation: " + e.getMessage(), e);
			return false;
		}
		return false;
	}

}
