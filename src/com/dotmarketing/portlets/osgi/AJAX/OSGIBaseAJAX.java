package com.dotmarketing.portlets.osgi.AJAX;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageUtil;

abstract class OSGIBaseAJAX extends AjaxAction {

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

			meth = this.getClass().getMethod(cmd, partypes);

		} catch (Exception e) {

			try {
				cmd = "action";
				meth = this.getClass().getMethod(cmd, partypes);
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

		response.getWriter().println("SUCCESS:" + success );
	}
}
