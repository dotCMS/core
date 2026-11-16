package com.dotmarketing.portal.struts;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.util.Logger;

import com.liferay.portal.struts.Action;
import com.liferay.portal.struts.ActionException;
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

	/**
	 * Value older versions stored as the local password of users synced from an external directory.
	 * The directory integration is gone, so nothing writes it any more, and it never matches as a
	 * password.
	 */
	public static final String FAKE_PASSWORD = "fake_dotCMS_LDAP_password";

	/**
	 * Runs after a back-end login and restores the page the user was trying to reach, if any.
	 *
	 * @param request  the login request
	 * @param response the login response
	 */
	@Override
	@SuppressWarnings({ "unchecked" })
	public void run(HttpServletRequest request, HttpServletResponse response)
	throws ActionException {

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
		

		
	}



}
