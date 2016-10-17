package com.dotmarketing.portal.struts;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.enterprise.LDAPImpl;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.Action;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.CookieUtil;
import com.liferay.util.ObjectValuePair;
import com.liferay.util.StringPool;

/**
 * This class is added to manage the flyout menues
 * @author Oswaldo Gallango
 * @since 1.5
 * @version 1.0
 */
public class DotCustomLoginPostAction extends Action {

	public static final String FAKE_PASSWORD = "fake_dotCMS_LDAP_password";
	
	@SuppressWarnings({ "unchecked" })
	public void run(HttpServletRequest request, HttpServletResponse response)
	throws ActionException {

		try{
			boolean SYNC_PASSWORD = Boolean.valueOf(PropsUtil.get("auth.impl.ldap.syncPassword"));
			if(!SYNC_PASSWORD){
				User user = com.liferay.portal.util.PortalUtil.getUser(request);
				Role r = com.dotmarketing.business.APILocator.getRoleAPI().loadRoleByKey(LDAPImpl.LDAP_USER_ROLE);
				if(com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, r)){
					user.setPassword(FAKE_PASSWORD);
					APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
				}
			}
		}catch (Exception e) {
			Logger.debug(this,"syncPassword not set or unable to load user", e);
		}
		
		
		
		try {
			// To manually set a path for the user to forward to, edit
			// portal.properties and set auth.forward.by.last.path to true.
	
				String mi =	CookieUtil.get(request.getCookies(), "backend_login_return_url");
				if(mi!=null)
					if(!mi.equals(StringPool.BLANK)){
						
						Cookie cookie = new Cookie("backend_login_return_url", StringPool.BLANK);
						cookie.setMaxAge(0);
						cookie.setPath("/");
						
						Map parameterMap = new LinkedHashMap();
						
						int j = 0;
						String [] parameters = mi.split("&");
						while(j<parameters.length){
							
							String[] p_and_v =parameters[j].split("=");
							parameterMap.put(p_and_v[0], new String[] {p_and_v[1]});
							j++;
						}
						
						ObjectValuePair obj=	new ObjectValuePair("/portal/layout", new LinkedHashMap(parameterMap));
						request.getSession().setAttribute(WebKeys.LAST_PATH, obj);
					}	
			


		}catch (Exception e) {
			Logger.warn(this, "ERROR: "+e.getMessage());
		}
		
		//Enabling pages edit mode
		HttpSession session = request.getSession();
		session.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, "true");
		session.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, null);
		session.setAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION, "true");		
		
	}



}
