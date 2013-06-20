package com.dotcms.publisher.endpoint.ajax;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

public class PublishingEndpointAjaxAction extends AjaxAction {

	@Override
	public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		return;
	}

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String, String> map = getURIParams();
		String cmd = map.get("cmd");
		Method dispatchMethod = null;

		User user = getUser();

		try{
			// Check permissions if the user has access to the CMS Maintenance Portlet
			if (user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", user)) {
				String userName = map.get("u") !=null
					? map.get("u")
						: map.get("user") !=null
							? map.get("user")
								: null;

				String password = map.get("p") !=null
					? map.get("p")
							: map.get("passwd") !=null
								? map.get("passwd")
									: null;



				LoginFactory.doLogin(userName, password, false, request, response);
				user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
				if(user==null) {
				    setUser(request);
	                user = getUser();
				}
				if(user==null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CONTENT_PUBLISHING_TOOL", user)){
					response.sendError(401);
					return;
				}
			}
		}
		catch(Exception e){
			Logger.error(this.getClass(), e.getMessage());
			response.sendError(401);
			return;
		}


		if(null!=cmd){
			try {
				dispatchMethod = this.getClass().getMethod(cmd, new Class[]{HttpServletRequest.class, HttpServletResponse.class});
			} catch (Exception e) {
				try {
					dispatchMethod = this.getClass().getMethod("action", new Class[]{HttpServletRequest.class, HttpServletResponse.class});
				} catch (Exception e1) {
					Logger.error(this.getClass(), "Trying to get method:" + cmd);
					Logger.error(this.getClass(), e1.getMessage(), e1.getCause());
					throw new DotRuntimeException(e1.getMessage());
				}
			}
			try {
				dispatchMethod.invoke(this, new Object[]{request,response});
			} catch (Exception e) {
				Logger.error(this.getClass(), "Trying to invoke method:" + cmd);
				Logger.error(this.getClass(), e.getMessage(), e.getCause());
				throw new DotRuntimeException(e.getMessage());
			}
		}
	}

	public void addEndpoint(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, LanguageException {
        try {
        	String identifier = request.getParameter("identifier");
        	if(UtilMethods.isSet(identifier)){
        		editEndpoint(request, response);
        		return;
        	}

        	String serverName = request.getParameter("serverName");
        	PublishingEndPoint existingServer = APILocator.getPublisherEndPointAPI().findEndPointByName(serverName);

        	if(existingServer!=null) {

        		Logger.info(getClass(), "Can't save EndPoint. An Endpoint with the given name already exists. ");
        		User user = getUser();
    			response.getWriter().println("FAILURE: " + LanguageUtil.get(user, "publisher_Endpoint_name_exists"));
    			return;
        	}

        	PublishingEndPoint endpoint = new PublishingEndPoint();
        	endpoint.setServerName(new StringBuilder(serverName));
        	endpoint.setAddress(request.getParameter("address"));
        	endpoint.setPort(request.getParameter("port"));
        	endpoint.setProtocol(request.getParameter("protocol"));
        	endpoint.setAuthKey(new StringBuilder(PublicEncryptionFactory.encryptString(request.getParameter("authKey"))));
        	endpoint.setEnabled(null!=request.getParameter("enabled"));
        	String sending = request.getParameter("sending");
        	endpoint.setSending("true".equals(sending));
        	endpoint.setGroupId(request.getParameter("environmentId"));
        	//Save the endpoint.
        	PublishingEndPointAPI peAPI = APILocator.getPublisherEndPointAPI();
        	peAPI.saveEndPoint(endpoint);



		} catch (DotDataException e) {
			Logger.info(getClass(), "Error saving EndPoint. Error Message: " +  e.getMessage());
			response.getWriter().println("FAILURE: " + e.getMessage());
		}
	}

	public void editEndpoint(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, LanguageException {
		try {

			String serverName = request.getParameter("serverName");
			String id = request.getParameter("identifier");
        	PublishingEndPoint existingServer = APILocator.getPublisherEndPointAPI().findEndPointByName(serverName);

        	if(existingServer!=null && !id.equals(existingServer.getId())) {

        		Logger.info(getClass(), "Can't save EndPoint. An Endpoint with the given name already exists. ");
        		User user = getUser();
    			response.getWriter().println("FAILURE: " + LanguageUtil.get(user, "publisher_Endpoint_name_exists"));
    			return;
        	}


			PublishingEndPoint endpoint = new PublishingEndPoint();
	        endpoint.setId(id);
			endpoint.setServerName(new StringBuilder(request.getParameter("serverName")));
			endpoint.setAddress(request.getParameter("address"));
			endpoint.setPort(request.getParameter("port"));
			endpoint.setProtocol(request.getParameter("protocol"));
			endpoint.setAuthKey(new StringBuilder(PublicEncryptionFactory.encryptString(request.getParameter("authKey"))));
			endpoint.setEnabled(null!=request.getParameter("enabled"));
			endpoint.setSending("true".equals(request.getParameter("sending")));
			endpoint.setGroupId(request.getParameter("environmentId"));
			//Update the endpoint.
			PublishingEndPointAPI peAPI = APILocator.getPublisherEndPointAPI();
			peAPI.updateEndPoint(endpoint);

		} catch (DotDataException e) {
			Logger.info(getClass(), "Error editing EndPoint. Error Message: " +  e.getMessage());
			response.getWriter().println("FAILURE: " + e.getMessage());
		}

	}
}
