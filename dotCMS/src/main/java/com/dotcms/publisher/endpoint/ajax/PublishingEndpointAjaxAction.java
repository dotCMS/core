package com.dotcms.publisher.endpoint.ajax;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.bean.factory.PublishingEndPointFactory;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.PublishingEndPointValidationException;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.Lists;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PublishingEndpointAjaxAction extends AjaxAction {

    final PublishingEndPointFactory factory = new PublishingEndPointFactory();

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
			if (user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("maintenance", user)) {
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
				if(user==null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("publishing-queue", user)){
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
				throw new DotRuntimeException(e.getMessage(),e);
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

        	String protocol = request.getParameter("protocol");

            PublishingEndPoint endpoint = factory.getPublishingEndPoint(protocol);
            endpoint.setServerName(new StringBuilder(serverName));
        	endpoint.setAddress(request.getParameter("address"));
        	endpoint.setPort(request.getParameter("port"));
        	endpoint.setProtocol(request.getParameter("protocol"));
        	endpoint.setAuthKey(new StringBuilder(PublicEncryptionFactory.encryptString(request.getParameter("authKey"))));
        	endpoint.setEnabled(null!=request.getParameter("enabled"));
        	String sending = request.getParameter("sending");
        	endpoint.setSending("true".equals(sending));
        	endpoint.setGroupId(request.getParameter("environmentId"));
            //Validate.
            try {
                endpoint.validatePublishingEndPoint();
            } catch (PublishingEndPointValidationException e){
                throw new DotDataException(handlePublishingEndPointValidationException(e));
            }
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

        	final String protocol = request.getParameter("protocol");

            PublishingEndPoint endpoint = factory.getPublishingEndPoint(protocol);
            endpoint.setId(id);
			endpoint.setServerName(new StringBuilder(request.getParameter("serverName")));
			endpoint.setAddress(request.getParameter("address"));
			endpoint.setPort(request.getParameter("port"));
			endpoint.setProtocol(request.getParameter("protocol"));
			endpoint.setAuthKey(new StringBuilder(PublicEncryptionFactory.encryptString(request.getParameter("authKey"))));
			endpoint.setEnabled(null!=request.getParameter("enabled"));
			endpoint.setSending("true".equals(request.getParameter("sending")));
			endpoint.setGroupId(request.getParameter("environmentId"));
			//Validate.
			try {
                endpoint.validatePublishingEndPoint();
            } catch (PublishingEndPointValidationException e){
                throw new DotDataException(handlePublishingEndPointValidationException(e));
            }

			//Update the endpoint.
			PublishingEndPointAPI peAPI = APILocator.getPublisherEndPointAPI();
			peAPI.updateEndPoint(endpoint);

		} catch (DotDataException e) {
			Logger.info(getClass(), "Error editing EndPoint. Error Message: " +  e.getMessage());
			response.getWriter().println("FAILURE: " + e.getMessage());
		}
	}

    /**
     * Iterates over all i18nMessages in the Exception and translate them using {@link
     * LanguageUtil}.
     *
     * @return comma separated {@link String} with all the translations.
     */
    private String handlePublishingEndPointValidationException(
            final PublishingEndPointValidationException e) {

        final List<String> i18nMessages = e.getI18nmessages();
        final List<String> errorMessages = Lists.newArrayList();

        for (final String i18nMessage : i18nMessages) {
            try {
                errorMessages.add(LanguageUtil.get(getUser(), i18nMessage));
            } catch (LanguageException le) {
                //If we have a problem, at least display the message code.
                errorMessages.add(i18nMessage);
            }
        }
        return String.join(",", errorMessages);
    }

}
