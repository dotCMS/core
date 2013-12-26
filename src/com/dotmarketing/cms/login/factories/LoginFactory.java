package com.dotmarketing.cms.login.factories;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.enterprise.BaseAuthenticator;
import com.dotcms.enterprise.LDAPImpl;
import com.dotcms.enterprise.salesforce.SalesForceUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.cms.login.struts.LoginForm;
import com.dotmarketing.portal.struts.DotCustomLoginPostAction;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.Validator;

/**
 * @author will
 *
 */
public class LoginFactory {
	
	public static String PRE_AUTHENTICATOR = PropsUtil.get("auth.pipeline.pre");
	
	/*Custom Code*/
	public static boolean useSalesForceLoginFilter = new Boolean (Config.getBooleanProperty("SALESFORCE_LOGIN_FILTER_ON",false));
	/*End of Custom Code*/
	
    public static boolean doLogin(LoginForm form, HttpServletRequest request, HttpServletResponse response) throws NoSuchUserException {
        return doLogin(form.getUserName(), form.getPassword(), form.isRememberMe(), request, response);

    }

    public static boolean doCookieLogin(String encryptedId, HttpServletRequest request, HttpServletResponse response) {

        try {
            String decryptedId = PublicEncryptionFactory.decryptString(encryptedId);           
            /*Custom Code*/
            User user = null;
            if(Validator.isEmailAddress(decryptedId))
                user = APILocator.getUserAPI().loadByUserByEmail(decryptedId,APILocator.getUserAPI().getSystemUser(),false);
             else
                user = APILocator.getUserAPI().loadUserById(decryptedId,APILocator.getUserAPI().getSystemUser(),false);
            /* End of Custom Code */
            try {
                String userName = user.getEmailAddress();
                Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();
                if (comp.getAuthType().equals(Company.AUTH_TYPE_ID)) {
                	userName = user.getUserId();
                }

                return doLogin(userName, user.getPassword(), true, request, response);
            } catch (Exception e) { // $codepro.audit.disable logExceptions
        		SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login (No user found) from IP: " + request.getRemoteAddr() + " :  " + e );

            	return false;
            }
        } catch (Exception e) {
    		SecurityLogger.logInfo(LoginFactory.class,"Auto login failed (No user found) from IP: " + request.getRemoteAddr() + " :  " + e );

            
            if(useSalesForceLoginFilter){
            	String decryptedId = PublicEncryptionFactory.decryptString(encryptedId);
            	Logger.info(LoginFactory.class, "Try to retrieve user from SalesForce with id: " + decryptedId);
            	User newUser = SalesForceUtils.migrateUserFromSalesforce(decryptedId, request,  response, true);

            	if(UtilMethods.isSet(newUser)){
            		 User user = null;
            		 Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();
                     try {
             			if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
        	            	user = APILocator.getUserAPI().loadByUserByEmail(decryptedId, APILocator.getUserAPI().getSystemUser(), false);
        	            } else {
        	            	user = APILocator.getUserAPI().loadUserById(decryptedId, APILocator.getUserAPI().getSystemUser(), false);
        	            }
             			
              	  		String instanceURL = request.getSession().getAttribute(SalesForceUtils.INSTANCE_URL).toString();
              	  		String accessToken = request.getSession().getAttribute(SalesForceUtils.ACCESS_TOKEN).toString();
            		
                  	  	if(UtilMethods.isSet(accessToken) && UtilMethods.isSet(instanceURL)){
                  	  		SalesForceUtils.syncRoles(user.getEmailAddress(), request, response, accessToken, instanceURL);
                  	  	}
                         
                        SalesForceUtils.setUserValuesOnSession(user, request, response, true);
                         
                        return true;
                         
                     } catch (Exception ex) { // $codepro.audit.disable logExceptions
     	        		SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login to salesforce from IP: " + request.getRemoteAddr());

                     	return false;
                     }
            	}
            	else
            		SecurityLogger.logInfo(LoginFactory.class, "Unable to retrieve user from SalesForce with id: " + decryptedId);
            		
        }

        doLogout(request, response);

        return false;
        
        }
    }

    /**
     *
     * @param userName
     * @param password
     * @param rememberMe
     * @param request
     * @param response
     * @return
     */
    public static boolean doLogin(String userName, String password, boolean rememberMe, HttpServletRequest request, HttpServletResponse response) throws NoSuchUserException {
        try {
        	User user = null;
        	boolean match = false;
        	Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();
        	
        	if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
				if(userName.equalsIgnoreCase(APILocator.getUserAPI().getSystemUser().getEmailAddress())){
	        		SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login with email as " + userName + " from IP: " + request.getRemoteAddr());

					return false;
				}
			} else {
				if(userName.equalsIgnoreCase(APILocator.getUserAPI().getSystemUser().getUserId())){
	        		SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login with userID as " + userName + " from IP: " + request.getRemoteAddr());

					return false;
				}
			}
        	
        	if ((PRE_AUTHENTICATOR != null) &&
        		(0 < PRE_AUTHENTICATOR.length()) &&
        		PRE_AUTHENTICATOR.equals(Config.getStringProperty("LDAP_FRONTEND_AUTH_IMPLEMENTATION"))) {
        		Class ldap_auth_impl_class = Class.forName(Config.getStringProperty("LDAP_FRONTEND_AUTH_IMPLEMENTATION"));
        		Authenticator ldap_auth_impl = (Authenticator) ldap_auth_impl_class.newInstance();
        		int auth = 0;
        		
    			if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
    				auth = ldap_auth_impl.authenticateByEmailAddress(comp.getCompanyId(), userName, password);
				} else {
					auth = ldap_auth_impl.authenticateByUserId(comp.getCompanyId(), userName, password);
				}
        		
    			if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
	            	user = APILocator.getUserAPI().loadByUserByEmail(userName, APILocator.getUserAPI().getSystemUser(), false);
	            } else {
	            	user = APILocator.getUserAPI().loadUserById(userName, APILocator.getUserAPI().getSystemUser(), false);
	            }
    			
    			try{
    				boolean SYNC_PASSWORD = BaseAuthenticator.SYNC_PASSWORD;
    				if(!SYNC_PASSWORD){
    					String roleName = LDAPImpl.LDAP_USER_ROLE;
    					if(com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, roleName)){
    						user.setPassword(DotCustomLoginPostAction.FAKE_PASSWORD);
    						APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
    					}
    				}
    			}catch (Exception e) {
    				Logger.debug(LoginFactory.class, "syncPassword not set or unable to load user", e);
    			}
    			
    			match = auth == Authenticator.SUCCESS;
        	} else {
	            if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
	            	user = APILocator.getUserAPI().loadByUserByEmail(userName, APILocator.getUserAPI().getSystemUser(), false);
	            } else {
	            	user = APILocator.getUserAPI().loadUserById(userName, APILocator.getUserAPI().getSystemUser(), false);
	            }
	            
	            if ((user == null) || (!UtilMethods.isSet(user.getEmailAddress()))) {
	        		SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login or no email set for " + userName + " from IP: " + request.getRemoteAddr());

	            	throw new NoSuchUserException();
	            }
	            
	            if (user.isNew() || 
	            		(!Config.getBooleanProperty("ALLOW_INACTIVE_ACCOUNTS_TO_LOGIN", false) && !user.isActive())) {
	        		SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login to an inactive account as " + userName + " from IP: " + request.getRemoteAddr());

	            	return false;
	            }
	            
	            match = user.getPassword().equals(password) || user.getPassword().equals(PublicEncryptionFactory.digestString(password));
	            
	            if (match) {
	            	if(useSalesForceLoginFilter){/*Custom Code */
	            		user = SalesForceUtils.migrateUserFromSalesforce(userName, request,  response, false);
	            	
          	  		String instanceURL = request.getSession().getAttribute(SalesForceUtils.INSTANCE_URL).toString();
          	  		String accessToken = request.getSession().getAttribute(SalesForceUtils.ACCESS_TOKEN).toString();
          	  		
              	  		if(UtilMethods.isSet(accessToken) && UtilMethods.isSet(instanceURL)){
              	  			SalesForceUtils.syncRoles(user.getEmailAddress(), request, response, accessToken, instanceURL);
              	  		}
              		}/*End of Custom Code*/
	            	user.setLastLoginDate(new java.util.Date());
	            	APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
	            	
	            } else {
	            	/*Custom code*/
	            	if(useSalesForceLoginFilter && user.getPassword().equalsIgnoreCase(SalesForceUtils.PASSWORD)){
	            		boolean saveSalesForceInfoInDotCMSLog = new Boolean (APILocator.getPluginAPI().loadProperty("com.dotcms.salesforce.plugin", "save_log_info_dotcms_log"));
	            		boolean saveSalesForceInfoInUserActivityLog = new Boolean (APILocator.getPluginAPI().loadProperty("com.dotcms.salesforce.plugin", "save_log_info_useractivity_log"));
	            		        		
	            		boolean isBoundToSalesforceServer = SalesForceUtils.accessSalesForceServer(request, response, user.getEmailAddress());
	            		
	            		if(isBoundToSalesforceServer){
	            			if(saveSalesForceInfoInDotCMSLog){
	            				Logger.info(LoginFactory.class, "dotCMS-Salesforce Plugin: User " + user.getEmailAddress()  
	            						+ " was able to connect to Salesforce server from IP: " + request.getRemoteAddr());
	            			}
	            			if(saveSalesForceInfoInUserActivityLog){
	            				SecurityLogger.logInfo(LoginFactory.class, "dotCMS-Salesforce Plugin :" + 
	            						"User " + user.getEmailAddress()  + " was able to connect to Salesforce server from IP: " + request.getRemoteAddr());
	            			}
                  	  		String instanceURL = request.getSession().getAttribute(SalesForceUtils.INSTANCE_URL).toString();
                  	  		String accessToken = request.getSession().getAttribute(SalesForceUtils.ACCESS_TOKEN).toString();
	            		
	                  	  	if(UtilMethods.isSet(accessToken) && UtilMethods.isSet(instanceURL)){
	                  	  		match = true;
	                  	  	}
	            		}
	            	}
	            	/* end of custom code*/
	            	else{
	            		match = false;
		            	user.setFailedLoginAttempts(user.getFailedLoginAttempts()+1);
		            	APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
		        		SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login as " + userName + " from IP: " + request.getRemoteAddr());

		            	
	            	}
	            }
        	}
        	
            // if passwords match
            if (match) {
            	HttpSession ses = request.getSession();
            	
                // session stuff
                ses.setAttribute(WebKeys.CMS_USER, user);
                
                //set personalization stuff on session
                
                // set id cookie
        		Cookie autoLoginCookie = UtilMethods.getCookie(request.getCookies(), WebKeys.CMS_USER_ID_COOKIE);
        		
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
                	response.addCookie(autoLoginCookie);
                }

        		SecurityLogger.logInfo(LoginFactory.class,"User " + userName + " has sucessfully login from IP: " + request.getRemoteAddr());

                return true;
            }
        } catch (NoSuchUserException e) {
			SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login as " + userName + " has been made from IP: " + request.getRemoteAddr());
        	throw e;
        } catch (Exception e) {
            Logger.error(LoginFactory.class, "Login Failed: " + e);
			SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login as " + userName + " has been made from IP: " + request.getRemoteAddr());
        }

        return false;
    }
    
    
    /**
    *
    * @param userName
    * @param password
    * @param rememberMe
    * @param request
    * @param response
    * @return
    */
    public static boolean doLogin(String userName, String password) throws NoSuchUserException {
        try {
        	User user = null;
        	boolean match = false;
        	Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();

        	if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
 				if(userName.equalsIgnoreCase(APILocator.getUserAPI().getSystemUser().getEmailAddress())){
 					SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login as " + userName + " has been made  - you cannot login as the system user");

 					return false;
 				}
 			} else {
 				if(userName.equalsIgnoreCase(APILocator.getUserAPI().getSystemUser().getUserId())){
 					SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login as " + userName + " has been made  - you cannot login as the system user");

 					return false;
 				}
 			}

        	if ((PRE_AUTHENTICATOR != null) &&
        		(0 < PRE_AUTHENTICATOR.length()) &&
        		PRE_AUTHENTICATOR.equals(Config.getStringProperty("LDAP_FRONTEND_AUTH_IMPLEMENTATION"))) {
        		Class ldap_auth_impl_class = Class.forName(Config.getStringProperty("LDAP_FRONTEND_AUTH_IMPLEMENTATION"));
        		Authenticator ldap_auth_impl = (Authenticator) ldap_auth_impl_class.newInstance();
        		int auth = 0;

    			if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
    				auth = ldap_auth_impl.authenticateByEmailAddress(comp.getCompanyId(), userName, password);
 				} else {
 					auth = ldap_auth_impl.authenticateByUserId(comp.getCompanyId(), userName, password);
 				}

    			if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
 	            	user = APILocator.getUserAPI().loadByUserByEmail(userName, APILocator.getUserAPI().getSystemUser(), false);
 	            } else {
 	            	user = APILocator.getUserAPI().loadUserById(userName, APILocator.getUserAPI().getSystemUser(), false);
 	            }

    			try{
    				boolean SYNC_PASSWORD = BaseAuthenticator.SYNC_PASSWORD;
    				if(!SYNC_PASSWORD){
    					String roleName = LDAPImpl.LDAP_USER_ROLE;
    					if(com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, roleName)){
    						user.setPassword(DotCustomLoginPostAction.FAKE_PASSWORD);
    						APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
    					}
    				}
    			}catch (Exception e) {
    				Logger.debug(LoginFactory.class, "syncPassword not set or unable to load user", e);
    			}

    			match = auth == Authenticator.SUCCESS;
        	} else {
 	            if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
 	            	user = APILocator.getUserAPI().loadByUserByEmail(userName, APILocator.getUserAPI().getSystemUser(), false);
 	            } else {
 	            	user = APILocator.getUserAPI().loadUserById(userName, APILocator.getUserAPI().getSystemUser(), false);
 	            }

 	            if ((user == null) || (!UtilMethods.isSet(user.getEmailAddress()))) {
 					SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login as " + userName + " has been made  - user cannot be found");

 	            	throw new NoSuchUserException();
 	            }

 	            if (user.isNew() ||
 	            		(!Config.getBooleanProperty("ALLOW_INACTIVE_ACCOUNTS_TO_LOGIN", false) && !user.isActive())) {
 					SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login as " + userName + " has been made  - user is marked inactive");

 	            	return false;
 	            }

 	            match = user.getPassword().equals(password) || user.getPassword().equals(PublicEncryptionFactory.digestString(password));

 	            if (match) {
 	            	user.setLastLoginDate(new java.util.Date());
 	            	APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
 	            } else {
 	            	user.setFailedLoginAttempts(user.getFailedLoginAttempts()+1);
 	            	APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
 	            }
        	}

            // if passwords match
            if (match) {
                return true;
            }
        } catch (NoSuchUserException e) {
			SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login as " + userName + " has been made :" + e);

        	throw e;
        } catch (Exception e) {
			SecurityLogger.logInfo(LoginFactory.class,"An invalid attempt to login as " + userName + " has been made :" + e);

        }

        return false;
    }

    public static void doLogout(HttpServletRequest request, HttpServletResponse response) {

        //request.getSession().invalidate();
        /*
         * request.getSession().removeAttribute(WebKeys.SESSION_USER);
         * request.getSession().removeAttribute(com.liferay.portal.util.WebKeys.USER_ID);
         * request.getSession().removeAttribute(com.liferay.portal.util.WebKeys.USER);
         */

        request.getSession().removeAttribute("PENDING_ALERT_SEEN");
        request.getSession().removeAttribute("createAccountForm");
        request.getSession().removeAttribute("checkoutForm");
        request.getSession().removeAttribute(WebKeys.CMS_USER);
        request.getSession().removeAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
        request.getSession().removeAttribute(WebKeys.LOGGED_IN_USER_CATS);
        request.getSession().removeAttribute(WebKeys.LOGGED_IN_USER_TAGS);
        request.getSession().removeAttribute(WebKeys.USER_FAVORITES);
        
        /*Custom Code*/
        if(useSalesForceLoginFilter){
        	request.getSession().removeAttribute(SalesForceUtils.ACCESS_TOKEN);
        	request.getSession().removeAttribute(SalesForceUtils.INSTANCE_URL);
        }
        /*End of custom code*/
        
        Cookie idCookie = new Cookie(WebKeys.CMS_USER_ID_COOKIE, null);
        idCookie.setMaxAge(0);
        idCookie.setPath("/");
        response.addCookie(idCookie);

    }
    
}
