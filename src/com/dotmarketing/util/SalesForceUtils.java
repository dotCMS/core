package com.dotmarketing.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DuplicateUserException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class SalesForceUtils {
	
	private static RoleAPI roleAPI = APILocator.getRoleAPI();
	private static UserAPI userAPI = APILocator.getUserAPI();
	public static User systemUser;
	public static Host systemHost;
	public static boolean saveSalesForceInfoInUserActivityLog;
	public static boolean saveSalesForceInfoInDotCMSLog;
	
	public static final String ACCESS_TOKEN = "salesforce.access.token";
	public static final String INSTANCE_URL = "salesforce.instance.url";
	public static final String PASSWORD = "dotCMSSalesForceFakePassword";
	
     
    public static boolean accessSalesForceServer(HttpServletRequest request, HttpServletResponse response, String login) throws DotDataException, DotSecurityException{
    	
    	systemUser = APILocator.getUserAPI().getSystemUser();
    	systemHost = APILocator.getHostAPI().findDefaultHost(systemUser, false);
    	saveSalesForceInfoInDotCMSLog = new Boolean (Config.getStringProperty("save_log_info_dotcms_log"));
		saveSalesForceInfoInUserActivityLog = new Boolean (Config.getStringProperty("save_log_info_useractivity_log"));
		
    	DefaultHttpClient client = new DefaultHttpClient();
    	
    	String requestUri = request.getRequestURI();

        try {
        	
          String salesForceTokenRequestURL = Config.getStringProperty("salesforce_token_request_url");
          String salesForceGrantType = Config.getStringProperty("salesforce_grant_type");
          String salesForceUserName = Config.getStringProperty("salesforce_username");
          String salesForcePassword = Config.getStringProperty("salesforce_password");
          String salesForceAPISecurityToken = Config.getStringProperty("salesforce_api_security_token");
          
          String clientId = "";
          String clientSecret = "";
          
			if(requestUri.contains("/admin")){
				clientId = Config.getStringProperty("salesforce_client_id_backend");
				clientSecret = Config.getStringProperty("salesforce_client_secret_backend");
			}
			else if(requestUri.contains("/dotCMS/login")){
				clientId = Config.getStringProperty("salesforce_client_id_frontend");
				clientSecret = Config.getStringProperty("salesforce_client_secret_frontend");
			}
          
          HttpPost post = new HttpPost(salesForceTokenRequestURL);
          
          List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
          nameValuePairs.add(new BasicNameValuePair("grant_type", salesForceGrantType));
          nameValuePairs.add(new BasicNameValuePair("client_id", clientId));
          nameValuePairs.add(new BasicNameValuePair("client_secret", clientSecret));
          nameValuePairs.add(new BasicNameValuePair("username",salesForceUserName));
          nameValuePairs.add(new BasicNameValuePair("password", salesForcePassword + salesForceAPISecurityToken));
          post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
          
          HttpResponse res = client.execute(post); 
          
          BufferedReader rd = new BufferedReader(new InputStreamReader(res.getEntity().getContent()));
          StringBuffer sb = new StringBuffer();
          String nl;
          while((nl = rd.readLine())!=null){
              sb.append(nl);
          }
          rd.close();
          
          JSONObject json = null;
          
          try{
        	  json = new JSONObject(sb.toString());
        	  String accessToken = json.getString("access_token");
        	  String instanceURL = json.getString("instance_url");
        	  if(UtilMethods.isSet(accessToken) && UtilMethods.isSet(instanceURL)){
        			request.getSession().setAttribute(INSTANCE_URL, accessToken);
        	    	request.getSession().setAttribute(ACCESS_TOKEN, accessToken);
        	  }
        	  
        	  return true;
          }
          catch (JSONException e){
			if(saveSalesForceInfoInDotCMSLog){
				Logger.error(SalesForceUtils.class, "dotCMS-SalesForce Plugin: " 
					+ "User " + login + 
					" was unable to connect to Salesforce server. "	+ e.getMessage());
				
			}
			if(saveSalesForceInfoInUserActivityLog){
				ActivityLogger.logInfo(SalesForceUtils.class, "dotCMS-SalesForce Plugin" , 
					"User " + login + 
					" was unable to connect to Salesforce server. "	+ e.getMessage(), 
						systemHost.getHostname());
			}
          }
          
        } catch (Exception e) {
			if(saveSalesForceInfoInDotCMSLog){
				Logger.error(SalesForceUtils.class, "dotCMS-SalesForce Plugin: " 
						+ "User " + login + 
						" was unable to connect to Salesforce server. "	+ e.getMessage());
				
			}
			if(saveSalesForceInfoInUserActivityLog){
				ActivityLogger.logInfo(SalesForceUtils.class, "dotCMS-SalesForce Plugin" , 
						"User " + login + 
						" was unable to connect to Salesforce server. "	+ e.getMessage(), 
						systemHost.getHostname());
			}

        }
        return false;
    }
    
	public static void syncRoles(String emailAddress, HttpServletRequest request, HttpServletResponse response, String accessToken, String instanceURL) {
		try{
			
			Map<String,String> userAttributes = retrieveUserInfoFromSalesforce(emailAddress, request, response);
			String salesforceSearchRoleField = Config.getStringProperty("salesforce_search_object_role_field");
			String RolesValues = userAttributes.get(salesforceSearchRoleField);
	        
	        if(UtilMethods.isSet(RolesValues)){
	        	
	        	String[] roleKeysToImport = RolesValues.split(";");
	        	int rolesSynced = 0;
	        	for(String roleKey: roleKeysToImport){
	        		Role role = null;
	        		try{
	        			User user = APILocator.getUserAPI().loadByUserByEmail(emailAddress, APILocator.getUserAPI().getSystemUser(), false);
	        			role = roleAPI.loadRoleByKey(roleKey);
	        			if(UtilMethods.isSet(role) && !roleAPI.doesUserHaveRole(user, role)){
	        				roleAPI.addRoleToUser(role, user);
	        				rolesSynced++;
	        			}
	        		}
	        		catch (Exception e){
						if(saveSalesForceInfoInDotCMSLog){
							Logger.error(SalesForceUtils.class, "dotCMS-SalesForce Plugin: " 
									+ "User " + emailAddress + 
									" was unable to sync roles with Salesforce server. "	+ e.getMessage());
							
						}
						if(saveSalesForceInfoInUserActivityLog){
							ActivityLogger.logInfo(SalesForceUtils.class, "dotCMS-SalesForce Plugin" , 
									"User " + emailAddress + 
									" was unable to sync roles with Salesforce server. "	+ e.getMessage(), 
									systemHost.getHostname());
						}
			        }
	        	}
	        	if(rolesSynced > 0){
					if(saveSalesForceInfoInDotCMSLog){
						Logger.info(SalesForceUtils.class, "dotCMS-SalesForce Plugin: " 
								+ rolesSynced +" Roles for User " + emailAddress  +
		        				" were synced with with Salesforce server.");
						
					}
					if(saveSalesForceInfoInUserActivityLog){
						ActivityLogger.logInfo(SalesForceUtils.class, "dotCMS-SalesForce Plugin" , 
								rolesSynced +" Roles for User " + emailAddress  +
		        				" were synced with with Salesforce server.",  
								systemHost.getHostname());
					}
	        	}

	        }
		}
		catch (Exception e){
			if(saveSalesForceInfoInDotCMSLog){
				Logger.error(SalesForceUtils.class, "dotCMS-SalesForce Plugin: " 
						+ "User " + emailAddress + 
						" was unable to sync roles with Salesforce server. "	+ e.getMessage());
				
			}
			if(saveSalesForceInfoInUserActivityLog){
				ActivityLogger.logInfo(SalesForceUtils.class, "dotCMS-SalesForce Plugin" , 
						"User " + emailAddress + 
						" was unable to sync roles with Salesforce server. "	+ e.getMessage(), 
						systemHost.getHostname());
			}
		}
	}	
	
	
	public static User migrateUserFromSalesforce(String emailAddress, HttpServletRequest request, HttpServletResponse response, boolean isNewUser){
		Map<String,String> userAttributes = retrieveUserInfoFromSalesforce(emailAddress, request, response);
		User liferayUser = null;
		
		if(userAttributes.size()>0){
			try {
				
				systemUser = userAPI.getSystemUser();
				
				if(isNewUser)
					liferayUser = userAPI.createUser("", emailAddress);
				else
					liferayUser = userAPI.loadByUserByEmail(emailAddress, systemUser, true);
				liferayUser.setActive(true);
				liferayUser.setFirstName(userAttributes.get("FirstName").toString());
				liferayUser.setLastName(userAttributes.get("LastName").toString());
				liferayUser.setCompanyId(PublicCompanyFactory.getDefaultCompanyId());
				liferayUser.setPassword(SalesForceUtils.PASSWORD);
				liferayUser.setCreateDate(new java.util.Date());
				userAPI.save(liferayUser, userAPI.getSystemUser(), true);
				return liferayUser;
			} catch (DuplicateUserException e) {
				Logger.error(SalesForceUtils.class, "Unable to add user " + liferayUser.getUserId() + " because it already exists on database");
				return null;
			} catch (Exception e) {
				Logger.error(SalesForceUtils.class, "Unable to add user " + liferayUser.getUserId() , e);
				return null;
			}
		} else{
			return null;
		}
	}
	
	public static Map<String,String> retrieveUserInfoFromSalesforce(String emailAddress, HttpServletRequest req, HttpServletResponse res){
		
		Map<String,String> attributes = new HashMap<String,String>();
		
		try{

			String salesforceSearchURL = Config.getStringProperty("salesforce_search_url");
			String salesforceSearchObject = Config.getStringProperty("salesforce_search_object");
			String salesforceSearchObjectFields = Config.getStringProperty("salesforce_search_object_fields");
			String salesforceSearchRoleField = Config.getStringProperty("salesforce_search_object_role_field");
			String accessToken = req.getSession().getAttribute(ACCESS_TOKEN).toString();
			String instanceURL = req.getSession().getAttribute(INSTANCE_URL).toString();
			
			if(!UtilMethods.isSet(salesforceSearchURL)){
				salesforceSearchURL = "/services/data/v26.0/search";
			}
			
			if(!UtilMethods.isSet(salesforceSearchObject)){
				salesforceSearchObject = "USER";
			}
			
			if(!UtilMethods.isSet(salesforceSearchObjectFields)){
				salesforceSearchObject = "FirstName,LastName,Email,ContactId";
			}
			
			String searchQuery = "FIND {" 
					+ emailAddress 
					+ "} " 
					+ "IN ALL FIELDS RETURNING "
					+ salesforceSearchObject
					+"("
					+ salesforceSearchObjectFields + ',' +salesforceSearchRoleField
					+")";
			
			searchQuery = UtilMethods.encodeURL(searchQuery);
			
			HttpClient httpclient = new HttpClient();
			
			GetMethod method = new GetMethod( instanceURL + salesforceSearchURL + "?q=" + searchQuery );
	        	          
	        method.setRequestHeader("Authorization", "Bearer " + accessToken);
	          
	        int status = httpclient.executeMethod(method); 
	        
	        if(status == HttpStatus.SC_OK){
	        	
	        	String result = method.getResponseBodyAsString();
	        		        
		        JSONArray jsonArray = null;
		        String FirstNameValue = "";
		        String LastNameValue = "";
		        String ContactIdValue = "";
		        String RolesValues="";
		          
		        try{
		        	jsonArray = new JSONArray(result);
		        	for(int i = 0; i < jsonArray.length(); i++) {
		        		JSONObject tempJSON = jsonArray.getJSONObject(i);
		        		FirstNameValue = tempJSON.get("FirstName").toString();
		        		LastNameValue = tempJSON.get("LastName").toString();
		        		ContactIdValue = tempJSON.get("ContactId").toString();
		        		RolesValues = tempJSON.get(salesforceSearchRoleField).toString();
		              }
		        }
		        catch (JSONException e){
					if(saveSalesForceInfoInDotCMSLog){
						Logger.error(SalesForceUtils.class, "dotCMS-SalesForce Plugin: " 
								+ "User " + emailAddress + 
								" was unable to get information from Salesforce. "	+ e.getMessage());
						
					}
					if(saveSalesForceInfoInUserActivityLog){
						ActivityLogger.logInfo(SalesForceUtils.class, "dotCMS-SalesForce Plugin" , 
								"User " + emailAddress + 
								" was unable to get information from Salesforce. "	+ e.getMessage(), 
								systemHost.getHostname());
					}
		        }
		        attributes.put("FirstName", FirstNameValue);
		        attributes.put("LastName", LastNameValue);
		        attributes.put("ContactId", ContactIdValue);
		        attributes.put(salesforceSearchRoleField, RolesValues);
	        }
		}
		catch (Exception e){
			if(saveSalesForceInfoInDotCMSLog){
				Logger.error(SalesForceUtils.class, "dotCMS-SalesForce Plugin: " 
						+ "User " + emailAddress + 
						" was unable to sync roles with Salesforce server. "	+ e.getMessage());
				
			}
			if(saveSalesForceInfoInUserActivityLog){
				ActivityLogger.logInfo(SalesForceUtils.class, "dotCMS-SalesForce Plugin" , 
						"User " + emailAddress + 
						" was unable to sync roles with Salesforce server. "	+ e.getMessage(), 
						systemHost.getHostname());
			}
		}

		return attributes;
		
	}
	
	public static void setUserValuesOnSession(User user, HttpServletRequest req, HttpServletResponse res, boolean rememberMe) throws PortalException, SystemException, DotDataException, DuplicateUserException, DotSecurityException{

		HttpSession ses = req.getSession();
    	
        // session stuff
        ses.setAttribute(WebKeys.CMS_USER, user);
        
        //set personalization stuff on session
        
        // set id cookie
		Cookie autoLoginCookie = UtilMethods.getCookie(req.getCookies(), WebKeys.CMS_USER_ID_COOKIE);
		
		if(autoLoginCookie == null && rememberMe) {
			autoLoginCookie = new Cookie(WebKeys.CMS_USER_ID_COOKIE, APILocator.getUserAPI().encryptUserId(user.getUserId()));
		}
		
        if (rememberMe) {
        	autoLoginCookie.setMaxAge(60 * 60 * 24 * 356);
        } else if (autoLoginCookie != null) {
        	autoLoginCookie.setMaxAge(0);
        }
        
        if (autoLoginCookie != null) {
			autoLoginCookie.setPath("/");
        	res.addCookie(autoLoginCookie);
        }

	}
	
}
