package com.dotmarketing.portlets.workflows.ajax;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.language.LanguageUtil;

@Deprecated
abstract class WfBaseAction extends AjaxAction {

	protected abstract Set<String> getAllowedCommands();

	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String cmd = request.getParameter("cmd");
		java.lang.reflect.Method meth = null;
		Class partypes[] = new Class[] { HttpServletRequest.class, HttpServletResponse.class };
		Object arglist[] = new Object[] { request, response };
		try {
			if (getUser() == null ) {
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
			Logger.error(WfBaseAction.class, "Trying to run method:" + cmd);
			Logger.error(WfBaseAction.class, e.getMessage(), e.getCause());
			writeError(response, e.getCause().getMessage());
		}

	}

	/**
	 * extracted as a separated method, so it can be verified when called
	 * @param method
	 * @param parameterTypes
	 * @return
	 * @throws NoSuchMethodException
	 */
	@VisibleForTesting
	Method getMethod(String method,  Class<?>... parameterTypes ) throws NoSuchMethodException {
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
