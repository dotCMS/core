package com.dotmarketing.portlets.osgi.AJAX;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

abstract class OSGIBaseAJAX extends AjaxAction {

    public boolean validateUser() {
        HttpServletRequest req = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        User user = null;
        try {
            user = com.liferay.portal.util.PortalUtil.getUser(req);
            if(user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("dynamic-plugins", user)){
                throw new DotSecurityException("User does not have access to the Plugin Portlet");
            }
            return true;
        } catch (Exception e) {
            Logger.error(this, e.getMessage());
            throw new DotRuntimeException (e.getMessage());
        }
    }


	protected abstract Set<String> getAllowedCommands();
    
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		if(System.getProperty(WebKeys.OSGI_ENABLED)==null){
    		return ;
    	}
		
		String cmd = request.getParameter("cmd");
		java.lang.reflect.Method meth = null;
		Class partypes[] = new Class[] { HttpServletRequest.class, HttpServletResponse.class };
		Object arglist[] = new Object[] { request, response };
		try {
		    if(getUser()==null || !doesUserHaveAccessToPortlet(getUser())){
				response.sendError(401);
				return;
			}

			if(getAllowedCommands().contains(cmd)) {
				meth = getMethod(cmd, partypes);
			}	else  if (UtilMethods.isSet(cmd)){
				Logger.error(this.getClass(), String.format("Attempt to run Invalid command %s :",cmd));
				return;
			}

		} catch (Exception e) {

			try {
				cmd = "action";
				meth = getMethod(cmd, partypes);
			} catch (Exception ex) {
				Logger.error(this.getClass(), "Trying to run method:" + cmd);
				Logger.error(this.getClass(), e.getMessage(), e.getCause());
				writeError(response, e.getCause().getMessage());
				return;
			}
		}
		try {
			meth.invoke(this, arglist);
		} catch (Exception e) {
			Logger.error(this, "Trying to run method:" + cmd);
			Logger.error(this, e.getMessage(), e.getCause());
			writeError(response, e.getCause().getMessage());
		}

	}

	/**
	 * Extracted so it can be mocked
	 * @param user
	 * @return
	 * @throws DotDataException
	 */
	@VisibleForTesting
	boolean doesUserHaveAccessToPortlet(final User user) throws DotDataException {
	  return APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("dynamic-plugins", user);
	}

	/**
	 * extracted as a separated method, so it can be verified when called
	 * @param method
	 * @param parameterTypes
	 * @return
	 * @throws NoSuchMethodException
	 */
	@VisibleForTesting
	Method getMethod(String method, Class<?>... parameterTypes ) throws NoSuchMethodException {
		return this.getClass().getMethod(method, parameterTypes);
	}


	public void writeError(HttpServletResponse response, String error) throws IOException {
		String ret = null;

		try {
			ret = LanguageUtil.get(getUser(), error);
		} catch (Exception e) {

		}
		if (ret == null) {
			try {
				ret = LanguageUtil.get(PublicCompanyFactory.getDefaultCompanyId(), PublicCompanyFactory.getDefaultCompany().getLocale(),
						error);
			} catch (Exception e) {

			}
		}
		if (ret == null) {
			ret = error;
		}
		response.setContentType("text/plain");
		response.getWriter().println("FAILURE: " + ret);
	}
	
	public void writeSuccess(HttpServletResponse response, String success) throws IOException {
		String ret = null;

		try {
			ret = LanguageUtil.get(getUser(), success);
		} catch (Exception e) {

		}
		if (ret == null) {
			try {
				ret = LanguageUtil.get(PublicCompanyFactory.getDefaultCompanyId(), PublicCompanyFactory.getDefaultCompany().getLocale(),
						success);
			} catch (Exception e) {

			}
		}
		if (ret == null) {
			ret = success;
		}
		response.setContentType("text/plain");
		response.getWriter().println("SUCCESS:" + success );
	}
}
