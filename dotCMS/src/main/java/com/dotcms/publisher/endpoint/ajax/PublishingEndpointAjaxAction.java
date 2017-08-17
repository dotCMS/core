package com.dotcms.publisher.endpoint.ajax;

import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Configuration;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3EndPointPublisher;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.repackage.com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.dotmarketing.business.APILocator;
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
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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



				APILocator.getLoginServiceAPI().doLogin(userName, password, false, request, response);
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


        	String protocol = request.getParameter("protocol");
        	String authKey = request.getParameter("authKey");
        	if (AWSS3Publisher.PROTOCOL_AWS_S3.equalsIgnoreCase(protocol)) {
        		validatePublishingEndPointAWSS3(authKey);
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


        	String protocol = request.getParameter("protocol");
        	String authKey = request.getParameter("authKey");
        	if (AWSS3Publisher.PROTOCOL_AWS_S3.equalsIgnoreCase(protocol)) {
        		validatePublishingEndPointAWSS3(authKey);
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

	private void validatePublishingEndPointAWSS3(String authKey) throws DotDataException, LanguageException {

		// Parse AWS S3 properties
		Properties props = new Properties();
		try {
			props.load( new StringReader( authKey ) );
		} catch (IOException e) {
			throw new DotDataException(
				LanguageUtil.get( getUser(), "publisher_Endpoint_awss3_authKey_format_invalid" )
			);
		}


		// Validate provision of all mandatory AWS S3 properties
		String token = props.getProperty(AWSS3Publisher.DOTCMS_PUSH_AWS_S3_TOKEN);
		String secret = props.getProperty(AWSS3Publisher.DOTCMS_PUSH_AWS_S3_SECRET);
		String bucketID = props.getProperty(AWSS3Publisher.DOTCMS_PUSH_AWS_S3_BUCKET_ID);
		String bucketValidationName = props.getProperty(AWSS3Publisher.DOTCMS_PUSH_AWS_S3_BUCKET_VALIDATION_NAME);

		if (!UtilMethods.isSet(bucketID)) {
			throw new DotDataException(
					LanguageUtil.get( getUser(), "publisher_Endpoint_awss3_authKey_missing_bucket_id" )
			);
		}

		if (!UtilMethods.isSet(token) || !UtilMethods.isSet(secret)) {
			// Validate DefaultAWSCredentialsProviderChain configuration
			DefaultAWSCredentialsProviderChain creds = new DefaultAWSCredentialsProviderChain();
			if (! new AWSS3EndPointPublisher(creds).canConnectSuccessfully(bucketValidationName)) {
				throw new DotDataException(
						LanguageUtil.get( getUser(), "publisher_Endpoint_DefaultAWSCredentialsProviderChain_invalid" )
				);
			}
		} else {
			// Validate correctness of AWS S3 connection properties
			AWSS3Configuration awss3Config =
					new AWSS3Configuration.Builder().accessKey(token).secretKey(secret).build();
			if (!new AWSS3EndPointPublisher(awss3Config).canConnectSuccessfully(bucketValidationName)) {
				throw new DotDataException(
						LanguageUtil.get(getUser(), "publisher_Endpoint_awss3_authKey_properties_invalid")
				);
			}
		}
	}
}
