package com.dotmarketing.filters;

import java.io.*;
import java.net.*;
import java.util.List;

import javax.portlet.WindowState;
import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DuplicateUserException;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.PreviewFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.RequiredLayoutException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.events.EventsProcessor;
import com.liferay.portal.model.User;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.PropsUtil;


public class SalesForceFilter implements Filter {
    
	public static User systemUser;
	public static Host systemHost;
	public static boolean saveSalesForceInfoInUserActivityLog;
	public static boolean saveSalesForceInfoInDotCMSLog;
	
	public static final String ACCESS_TOKEN = "salesforce.access.token";
	public static final String INSTANCE_URL = "salesforce.instance.url";

	private String clientId;
	private String clientSecret;
	private String redirectUri;
	private String environment;
	private String authUrl;
	private String tokenUrl;
	private String searchUsersURL;
    
    public void doFilter (ServletRequest req, ServletResponse res, FilterChain fc)
        throws ServletException, IOException {
    	
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
		HttpSession session = request.getSession(false);
		
    	String requestUri = request.getRequestURI();
    	
    	boolean useSalesForceLoginFilter = new Boolean (Config.getBooleanProperty("SALESFORCE_LOGIN_FILTER_ON",false));
    	
    	if(useSalesForceLoginFilter){
    		try {    		
    	    	saveSalesForceInfoInDotCMSLog = Config.getBooleanProperty("save_log_info_dotcms_log", false);
    			saveSalesForceInfoInUserActivityLog = Config.getBooleanProperty("save_log_info_useractivity_log",false);
    			
    			if(requestUri.equals("/admin") || requestUri.equals("/c")){
    				clientId = Config.getStringProperty("salesforce_client_id_backend");
    				clientSecret = Config.getStringProperty("salesforce_client_secret_backend");
    				redirectUri = Config.getStringProperty("salesforce_redirect_uri_backend");
    			}
    			else if(requestUri.contains("/dotCMS/login")){
    				clientId = Config.getStringProperty("salesforce_client_id_frontend");
    				clientSecret = Config.getStringProperty("salesforce_client_secret_frontend");
    				redirectUri = Config.getStringProperty("salesforce_redirect_uri_frontend");
    			}

    			environment = Config.getStringProperty("salesforce_environment");
    			
    			tokenUrl = Config.getStringProperty("salesforce_token_request_url");
    			
    			authUrl = environment
    					+ "/services/oauth2/authorize?response_type=code&client_id="
    					+ clientId + "&redirect_uri="
    					+ URLEncoder.encode(redirectUri, "UTF-8");
    			
    			searchUsersURL = Config.getStringProperty("salesforce_search_user_url");
    		      
    		} catch (UnsupportedEncodingException e) {
    			e.printStackTrace();
    		}

            // make sure we've got an HTTP request
            if (!(req instanceof HttpServletRequest) || !(res instanceof HttpServletResponse)) {
            	throw new ServletException("SalesForceFilter protects only HTTP resources");
            }
            
    		String accessToken = (String) request.getSession().getAttribute(ACCESS_TOKEN);
    		
    		String instanceUrl = "";
    		
    		String salesforceIdURI = ""; 
    		
            if (UtilMethods.isSet(accessToken)){
            	
    	        String encryptedId = UtilMethods.getCookieValue(request.getCookies(), WebKeys.CMS_USER_ID_COOKIE);
    	   	 
    	        if (((session != null && session.getAttribute(WebKeys.CMS_USER) == null) || session == null)&& 
    	        		UtilMethods.isSet(encryptedId)) {
    	            Logger.debug(SalesForceFilter.class, "Doing AutoLogin for " + encryptedId);
    	            LoginFactory.doCookieLogin(encryptedId, request, response);
    	        }
            }
            
            else if(UtilMethods.isSet(request.getParameter("code"))){
            	
            	String SalesForceUserEmailAddress = "";
            	
            	String code = request.getParameter("code");

    			HttpClient httpclient = new HttpClient();

    			PostMethod post = new PostMethod(tokenUrl);
    			post.addParameter("code", code);
    			post.addParameter("grant_type", "authorization_code");
    			post.addParameter("client_id", clientId);
    			post.addParameter("client_secret", clientSecret);
    			post.addParameter("redirect_uri", redirectUri);

    			try {
    				httpclient.executeMethod(post);

    				try {
    					JSONObject authResponse = new JSONObject(
    							new JSONTokener(new InputStreamReader(
    									post.getResponseBodyAsStream())));

    					accessToken = authResponse.getString("access_token");
    					instanceUrl = authResponse.getString("instance_url");
    					salesforceIdURI = authResponse.getString("id");

    				} catch (JSONException e) {
    					e.printStackTrace();
    					throw new ServletException(e);
    				}
    			} finally {
    				post.releaseConnection();
    			}
    			
    			String[] SalesForceUserInfo=salesforceIdURI.split("/");
    			
    			String SalesForceUserId = SalesForceUserInfo[SalesForceUserInfo.length-1];
    		
    			GetMethod get = new GetMethod(searchUsersURL + SalesForceUserId);
    			
    			get.setRequestHeader("Authorization", "OAuth " + accessToken);
    			
    			try {
    				httpclient.executeMethod(get);
    				if (get.getStatusCode() == HttpStatus.SC_OK) {
    					try {
    						JSONObject userJSONObject = new JSONObject(
    								new JSONTokener(new InputStreamReader(
    										get.getResponseBodyAsStream())));

    						SalesForceUserEmailAddress = userJSONObject.getString("Username");

    					} catch (JSONException e) {
    						e.printStackTrace();
    						throw new ServletException(e);
    					}
    				}
    			} finally {
    				get.releaseConnection();
    			}


    			// Set a session attribute so that other servlets can get the access
    			// token
    			request.getSession().setAttribute(ACCESS_TOKEN, accessToken);
    	
    			// We also get the instance URL from the OAuth response, so set it
    			// in the session too
    			request.getSession().setAttribute(INSTANCE_URL, instanceUrl);
    			
            	Logger.debug(AutoLoginFilter.class, "Doing SalesForceAutoLogin Filter for: " + SalesForceUserEmailAddress);
                if(UtilMethods.isSet(SalesForceUserEmailAddress)){                
                	LoginFactory.doCookieLogin(PublicEncryptionFactory.encryptString(SalesForceUserEmailAddress), request, response);      	
                }
                
                if(requestUri.equals("/admin"))
                	/*Backend redirecting*/
                {
                	setBackendSessionVariables(request,response);
                }
                else{
                	/*Frontend redirecting*/
        			response.sendRedirect(requestUri);
        			return;
                }
            }
            else {
    			response.sendRedirect(authUrl);
    			return;
            }
    		
    	}
    	

        // continue processing the request
        fc.doFilter(request, response);
        
    }

	private void setBackendSessionVariables(HttpServletRequest request,HttpServletResponse response) {
		
		try {
        	User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
			UserAPI userAPI = APILocator.getUserAPI();			
			boolean respectFrontend = WebAPILocator.getUserWebAPI().isLoggedToBackend(request);			
			//Locale userSelectedLocale = (Locale)request.getSession().getAttribute(Globals.LOCALE_KEY);			
			user.setLanguageId(user.getLocale().toString());
			userAPI.save(user, userAPI.getSystemUser(), respectFrontend);

			request.getSession().setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
			
			//DOTCMS-6392
			PreviewFactory.setVelocityURLS(request);
			
			//set the host to the domain of the URL if possible if not use the default host
			//http://jira.dotmarketing.net/browse/DOTCMS-4475
			try{
				String domainName = request.getServerName();
				Host h = null;
				h = APILocator.getHostAPI().findByName(domainName, user, false);
				if(h == null || !UtilMethods.isSet(h.getInode())){
					h = APILocator.getHostAPI().findByAlias(domainName, user, false);
				}
				if(h != null && UtilMethods.isSet(h.getInode())){
					request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, h.getIdentifier());
				}else{
					request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), true).getIdentifier());
				}
			}catch (DotSecurityException se) {
				request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), true).getIdentifier());
			}
						
			request.getSession().removeAttribute("_failedLoginName");
			Cookie idCookie = new Cookie(CookieKeys.ID,UserManagerUtil.encryptUserId(user.getUserId()));
			idCookie.setPath("/");

			idCookie.setMaxAge(0);
			
			response.addCookie(idCookie);

			EventsProcessor.process(PropsUtil.getArray(PropsUtil.LOGIN_EVENTS_PRE), request, response);
			EventsProcessor.process(PropsUtil.getArray(PropsUtil.LOGIN_EVENTS_POST), request, response);
			
			List<Layout> userLayouts;
			userLayouts = APILocator.getLayoutAPI().loadLayoutsForUser(user);
			if ((userLayouts == null) || (userLayouts.size() == 0) || !UtilMethods.isSet(userLayouts.get(0).getId())) {
				throw new RequiredLayoutException();
			}
			
			Layout layout = userLayouts.get(0);
			List<String> portletIds = layout.getPortletIds();
			String portletId = portletIds.get(0);
			java.util.Map<String, String[]> params = new java.util.HashMap<String, String[]>();
			params.put("struts_action",new String[] {"/ext/director/direct"});
			String directorURL = com.dotmarketing.util.PortletURLUtil.getActionURL(request,layout.getId(),WindowState.MAXIMIZED.toString(),params, portletId);
			request.getSession().setAttribute(com.dotmarketing.util.WebKeys.DIRECTOR_URL, directorURL);
			
		} catch (DotDataException e) {
			e.printStackTrace();
		} catch (PortalException e) {
			e.printStackTrace();
		} catch (SystemException e) {
			e.printStackTrace();
		} catch (DuplicateUserException e) {
			e.printStackTrace();
		} catch (DotSecurityException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void destroy() {
		
	}
	
	@Override
    public void init(FilterConfig config) throws ServletException {

    }

}
