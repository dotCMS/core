package com.dotmarketing.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

public class LoginEditModeServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5466263766587176193L;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if(request.getRequestURI().indexOf("edit") > -1){
			
			Identifier _edit_mode_id = new Identifier();
			_edit_mode_id.setURI("/"); 
			try{
				Clickstream clickstream = (Clickstream) request.getSession().getAttribute("clickstream");
				String pageId= clickstream.getLastPageId();
				_edit_mode_id = (Identifier) HibernateUtil.load(Identifier.class, pageId);
					

			}
			catch(Exception e){
				Logger.error(this.getClass(), "unable to get last page"  + e);
			}
			
			// this is used by the PortalRequestProcessort.java to set you up in edit mode
			request.getSession().setAttribute("LOGIN_TO_EDIT_MODE", _edit_mode_id);
		}
		else{
			request.getSession().removeAttribute("LOGIN_TO_EDIT_MODE");
			
		}
		HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
		Host host;
		try {
			host = hostWebAPI.getCurrentHost(request);
			request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, host.getIdentifier());
		} catch (PortalException e) {
			Logger.error(LoginEditModeServlet.class,e.getMessage(),e);
		} catch (SystemException e) {
			Logger.error(LoginEditModeServlet.class,e.getMessage(),e);
		} catch (DotDataException e) {
			Logger.error(LoginEditModeServlet.class,e.getMessage(),e);
		} catch (DotSecurityException e) {
			Logger.error(LoginEditModeServlet.class,e.getMessage(),e);
		}
		request.getRequestDispatcher("/c").forward(request,response);
	}
	
}
