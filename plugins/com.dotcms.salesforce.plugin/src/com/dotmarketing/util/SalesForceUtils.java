package com.dotmarketing.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;


public class SalesForceUtils {
	
	private static PluginAPI pluginAPI = APILocator.getPluginAPI();
	private static RoleAPI roleAPI = APILocator.getRoleAPI();
	public static User systemUser;
	public static Host systemHost;
	public static boolean saveSalesForceInfoInUserActivityLog;
	public static boolean saveSalesForceInfoInDotCMSLog;
	
	public static final String ACCESS_TOKEN = "salesforce.access.token";
	public static final String INSTANCE_URL = "salesforce.instance.url";
     
    public static boolean accessSalesForceServer(HttpServletRequest request, HttpServletResponse response, User user) throws DotDataException, DotSecurityException{
    	
    	systemUser = APILocator.getUserAPI().getSystemUser();
    	systemHost = APILocator.getHostAPI().findDefaultHost(systemUser, false);
    	saveSalesForceInfoInDotCMSLog = new Boolean (APILocator.getPluginAPI().loadProperty("com.dotcms.salesforce.plugin", "save_log_info_dotcms_log"));
		saveSalesForceInfoInUserActivityLog = new Boolean (APILocator.getPluginAPI().loadProperty("com.dotcms.salesforce.plugin", "save_log_info_useractivity_log"));
		
    	DefaultHttpClient client = new DefaultHttpClient();

        try {
        	
          String salesForceTokenRequestURL = pluginAPI.loadProperty("com.dotcms.salesforce.plugin","salesforce_token_request_url");
          String salesForceGrantType = pluginAPI.loadProperty("com.dotcms.salesforce.plugin","salesforce_grant_type");
          String salesForceClientId = pluginAPI.loadProperty("com.dotcms.salesforce.plugin","salesforce_client_id");
          String salesForceClientSecret = pluginAPI.loadProperty("com.dotcms.salesforce.plugin","salesforce_client_secret");
          String salesForceUserName = pluginAPI.loadProperty("com.dotcms.salesforce.plugin","salesforce_username");
          String salesForcePassword = pluginAPI.loadProperty("com.dotcms.salesforce.plugin","salesforce_password");
          String salesForceAPISecurityToken = pluginAPI.loadProperty("com.dotcms.salesforce.plugin","salesforce_api_security_token");
          
          HttpPost post = new HttpPost(salesForceTokenRequestURL);
          
          List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
          nameValuePairs.add(new BasicNameValuePair("grant_type", salesForceGrantType));
          nameValuePairs.add(new BasicNameValuePair("client_id", salesForceClientId));
          nameValuePairs.add(new BasicNameValuePair("client_secret", salesForceClientSecret));
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
        		  setValuesOnSession(request, instanceURL, accessToken);
        		  syncRoles(user,instanceURL, accessToken);
        	  }
        	  
        	  return true;
          }
          catch (JSONException e){
			if(saveSalesForceInfoInDotCMSLog){
				Logger.error(SalesForceUtils.class, "dotCMS-SalesForce Plugin: " 
					+ "User " + user.getEmailAddress() + 
					" was unable to connect to Salesforce server. "	+ e.getMessage());
				
			}
			if(saveSalesForceInfoInUserActivityLog){
				ActivityLogger.logInfo(SalesForceUtils.class, "dotCMS-SalesForce Plugin" , 
					"User " + user.getEmailAddress() + 
					" was unable to connect to Salesforce server. "	+ e.getMessage(), 
						systemHost.getHostname());
			}
          }
          
        } catch (Exception e) {
			if(saveSalesForceInfoInDotCMSLog){
				Logger.error(SalesForceUtils.class, "dotCMS-SalesForce Plugin: " 
						+ "User " + user.getEmailAddress() + 
						" was unable to connect to Salesforce server. "	+ e.getMessage());
				
			}
			if(saveSalesForceInfoInUserActivityLog){
				ActivityLogger.logInfo(SalesForceUtils.class, "dotCMS-SalesForce Plugin" , 
						"User " + user.getEmailAddress() + 
						" was unable to connect to Salesforce server. "	+ e.getMessage(), 
						systemHost.getHostname());
			}

        }
        return false;
    }
    
    
	private static void setValuesOnSession(HttpServletRequest request, String instanceURL, String accessToken){
    	
		request.getSession().setAttribute(INSTANCE_URL, accessToken);
    	request.getSession().setAttribute(ACCESS_TOKEN, accessToken);

	}
	
	private static void syncRoles(User user, String instanceURL, String accessToken) {
		try{
			
			String salesforceSearchURL = pluginAPI.loadProperty("com.dotcms.salesforce.plugin","salesforce_search_url");
			String salesforceSearchObject = pluginAPI.loadProperty("com.dotcms.salesforce.plugin","salesforce_search_object");
			String salesforceSearchObjectField = pluginAPI.loadProperty("com.dotcms.salesforce.plugin","salesforce_search_object_field");
			
			if(!UtilMethods.isSet(salesforceSearchURL)){
				salesforceSearchURL = "/services/data/v26.0/search";
			}
			
			if(!UtilMethods.isSet(salesforceSearchObject)){
				salesforceSearchObject = "CONTACT";
			}
			
			String searchQuery = "FIND {" + user.getEmailAddress() + "} " 
					+ "IN ALL FIELDS RETURNING " 
					+ salesforceSearchObject
					+ "(" + salesforceSearchObjectField + ")";
			
			searchQuery = UtilMethods.encodeURL(searchQuery);
			
			HttpClient httpclient = new HttpClient();
			
			GetMethod method = new GetMethod( instanceURL + salesforceSearchURL + "?q=" + searchQuery );
	        	          
	        method.setRequestHeader("Authorization", "Bearer " + accessToken);
	          
	        int res = httpclient.executeMethod(method); 
	        
	        if(res == HttpStatus.SC_OK){
	        	
	        	String result = method.getResponseBodyAsString();
	        		        
		        JSONArray jsonArray = null;
		        String objectType = "";
		        String value = "";
		          
		        try{
		        	jsonArray = new JSONArray(result);
		        	for(int i = 0; i < jsonArray.length(); i++) {
		        		JSONObject tempJSON = jsonArray.getJSONObject(i);
		        		value = tempJSON.get(salesforceSearchObjectField).toString();
		        		objectType = tempJSON.getJSONObject("attributes").getString("type");
		              }
		        }
		        catch (JSONException e){
					if(saveSalesForceInfoInDotCMSLog){
						Logger.error(SalesForceUtils.class, "dotCMS-SalesForce Plugin: " 
								+ "User " + user.getEmailAddress() + 
								" was unable to get information from Salesforce. "	+ e.getMessage());
						
					}
					if(saveSalesForceInfoInUserActivityLog){
						ActivityLogger.logInfo(SalesForceUtils.class, "dotCMS-SalesForce Plugin" , 
								"User " + user.getEmailAddress() + 
								" was unable to get information from Salesforce. "	+ e.getMessage(), 
								systemHost.getHostname());
					}
		        }
		        
		        if(UtilMethods.isSet(value)){
		        	String[] roleKeysToImport = value.split(";");
		        	int rolesSynced = 0;
		        	for(String roleKey: roleKeysToImport){
		        		Role role = null;
		        		try{
		        			role = roleAPI.loadRoleByKey(roleKey);
		        			if(UtilMethods.isSet(role) && !roleAPI.doesUserHaveRole(user, role)){
		        				roleAPI.addRoleToUser(role, user);
		        				rolesSynced++;
		        			}
		        		}
		        		catch (Exception e){
							if(saveSalesForceInfoInDotCMSLog){
								Logger.error(SalesForceUtils.class, "dotCMS-SalesForce Plugin: " 
										+ "User " + user.getEmailAddress() + 
										" was unable to sync roles with Salesforce server. "	+ e.getMessage());
								
							}
							if(saveSalesForceInfoInUserActivityLog){
								ActivityLogger.logInfo(SalesForceUtils.class, "dotCMS-SalesForce Plugin" , 
										"User " + user.getEmailAddress() + 
										" was unable to sync roles with Salesforce server. "	+ e.getMessage(), 
										systemHost.getHostname());
							}
				        }
		        	}
		        	if(rolesSynced > 0){
						if(saveSalesForceInfoInDotCMSLog){
							Logger.info(SalesForceUtils.class, "dotCMS-SalesForce Plugin: " 
									+ rolesSynced +" Roles for User " + user.getEmailAddress()  +
			        				" were synced with with Salesforce server.");
							
						}
						if(saveSalesForceInfoInUserActivityLog){
							ActivityLogger.logInfo(SalesForceUtils.class, "dotCMS-SalesForce Plugin" , 
									rolesSynced +" Roles for User " + user.getEmailAddress()  +
			        				" were synced with with Salesforce server.",  
									systemHost.getHostname());
						}
		        	}
		        }
	        }
		}
		catch (Exception e){
			if(saveSalesForceInfoInDotCMSLog){
				Logger.error(SalesForceUtils.class, "dotCMS-SalesForce Plugin: " 
						+ "User " + user.getEmailAddress() + 
						" was unable to sync roles with Salesforce server. "	+ e.getMessage());
				
			}
			if(saveSalesForceInfoInUserActivityLog){
				ActivityLogger.logInfo(SalesForceUtils.class, "dotCMS-SalesForce Plugin" , 
						"User " + user.getEmailAddress() + 
						" was unable to sync roles with Salesforce server. "	+ e.getMessage(), 
						systemHost.getHostname());
			}
		}
	}
}
