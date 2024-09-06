/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.cas;

import com.dotcms.enterprise.BaseAuthenticator;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.repackage.com.novell.ldap.LDAPAttribute;
import com.dotcms.repackage.com.novell.ldap.LDAPConnection;
import com.dotcms.repackage.com.novell.ldap.LDAPEntry;
import com.dotcms.repackage.com.novell.ldap.LDAPException;
import com.dotcms.repackage.com.novell.ldap.LDAPJSSESecureSocketFactory;
import com.dotcms.repackage.com.novell.ldap.LDAPResponse;
import com.dotcms.repackage.com.novell.ldap.LDAPResponseQueue;
import com.dotcms.repackage.com.novell.ldap.LDAPSearchResults;
import com.dotcms.repackage.com.novell.ldap.LDAPSocketFactory;
import com.dotmarketing.auth.model.UserAttribute;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portal.struts.DotCustomLoginPostAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class CASAuthUtils {
	
	private static UserAPI userAPI = APILocator.getUserAPI();
	private static RoleAPI roleAPI = APILocator.getRoleAPI();
	
    public static String INITIAL_CONTEXT_FACTORY = PropsUtil.get("com.sun.jndi.ldap.LdapCtxFactory");

    public static String SECURITY_AUTHENTICATION = PropsUtil.get("auth.impl.ldap.security.authentication");

    public static String SECURITY_KEYSTORE_PATH = PropsUtil.get("auth.impl.ldap.security.keystore.path");

    public static String HOST = PropsUtil.get("auth.impl.ldap.host");

    public static String PORT = PropsUtil.get("auth.impl.ldap.port");

    public static String USERID = PropsUtil.get("auth.impl.ldap.userid");

    public static String PASSWORD = PropsUtil.get("auth.impl.ldap.password");

    public static String DOMAINLOOKUP = PropsUtil.get("auth.impl.ldap.domainlookup");

    public static boolean IS_BUILD_GROUPS = Boolean.valueOf(PropsUtil.get("auth.impl.build.groups"));
 
	public static String GROUP_FILTER = PropsUtil.get("auth.impl.ldap.build.group.name.filter");

    public static String GROUP_STRING_TO_STRIP = PropsUtil.get("auth.impl.ldap.build.group.name.filter.strip");

    public static String LDAP_USER_ROLE = "LDAP User";

    public static String USER_ATTRIB = PropsUtil.get("auth.impl.ldap.attrib.user");

    public static String FIRST_NAME_ATTRIB = PropsUtil.get("auth.impl.ldap.attrib.firstName");

    public static String MIDDLE_NAME_ATTRIB = PropsUtil.get("auth.impl.ldap.attrib.middleName");

    public static String LAST_NAME_ATTRIB = PropsUtil.get("auth.impl.ldap.attrib.lastName");

    public static String NICKNAME_ATTRIB = PropsUtil.get("auth.impl.ldap.attrib.nickName");

    public static String EMAIL_ATTRIB = PropsUtil.get("auth.impl.ldap.attrib.email");

    public static String GENDER_ATTRIB = PropsUtil.get("auth.impl.ldap.attrib.gender");

    public static String GROUP_ATTRIB = PropsUtil.get("auth.impl.ldap.attrib.group");

    public static boolean SYNC_PASSWORD = Boolean.valueOf(PropsUtil.get("auth.impl.ldap.syncPassword"));
    
    private static UserAttribute userAttribute;
    private static User liferayUser;

    
	/* (non-Javadoc)
	 * @see com.dotcms.enterprise.BaseAuthenticator#loadGroups(java.lang.String, java.lang.String)
	 *
	 * Accepts: An LDAP username and password
	 * Requires:  LDAP should be using the mail attribute to store e-mail addresses
	 *			    and should store the group information in the isMemberOf attribute.
	 *			  The following entries in portal-ext.properties
	 *			  	auth.impl.build.groups=true
	 * 			  	auth.impl.ldap.build.group.name.filter=regex with selection.
	 * Outputs: A string list of group names that the given user is a member of.
	 */

	public static User loadUserFromLDAP(String userId) {
		
		String companyId = PublicCompanyFactory.getDefaultCompanyId();
		liferayUser = null;
		
		LDAPConnection lc = getBindedConnection();
		LDAPEntry ldapEntry;
		try{
			ldapEntry = searchUserinLDAP(lc, userId);
		}catch(NoSuchUserException nsu){
			Logger.error(CASAuthUtils.class, "Unable to find user in LDAP");
			return null;
		}
		
		userId = ldapEntry.getAttribute(USER_ATTRIB).getStringValue();
		String email = ldapEntry.getAttribute(EMAIL_ATTRIB).getStringValue();
		
		try{
			liferayUser = userAPI.createUser(userId, email);
			liferayUser.setActive(true);
			liferayUser.setCompanyId(companyId);
			liferayUser.setPassword(DotCustomLoginPostAction.FAKE_PASSWORD);			
			liferayUser.setCreateDate(new java.util.Date());
			
			userAPI.save(liferayUser,APILocator.getUserAPI().getSystemUser(),false);
	    	try{
	    		roleAPI.addRoleToUser(roleAPI.loadRoleByKey(LDAP_USER_ROLE), liferayUser);
	    	}catch(Exception ex){
	    		Logger.error(CASAuthUtils.class, "Unable to add user " + liferayUser.getUserId() + " to LDAP User role", ex);
	    	}
			Logger.debug(BaseAuthenticator.class, "User Migration Successfull");
		}catch(Exception ex){
			Logger.error(BaseAuthenticator.class,"Failed to create user " + userId + ": ",ex);
			return null;
		}
		try{
			userAttribute = loadUserAttributes(ldapEntry, userId);
			syncUserAttributes(userAttribute);
		}catch(Exception e){
			Logger.error(BaseAuthenticator.class, "Unable to load user attributes : ", e);
		}
        
		try {
			ArrayList<String> groups = loadUserGroups(liferayUser);
			syncUserGroups (liferayUser, groups);
		} catch (Exception e) {
			Logger.error(CASAuthUtils.class, "Unable to sync groups for user " + liferayUser.getUserId() , e);
		}
		return liferayUser;
	}
	
	public static ArrayList<String> loadUserGroups (User user){
		
		ArrayList<String> groups = new ArrayList<>();
		LDAPConnection lc = getBindedConnection();
		LDAPEntry ldapEntry;
		try{
			ldapEntry = searchUserinLDAP(lc, user.getUserId());
		}catch(NoSuchUserException nsu){
			Logger.error(CASAuthUtils.class, "Unable to find user in LDAP");
			return null;
		}
		LDAPAttribute a = ldapEntry.getAttribute(GROUP_ATTRIB);
		Enumeration values = a.getStringValues();
		while (values.hasMoreElements()) {
			String groupDN = (String)values.nextElement();
			String groupName = groupDN;
			try{
				groupName = groupDN.split(",")[0].split("=")[1];
			}catch(Exception ex){
				groupName = groupDN;
			}
			if(groupName.matches(GROUP_FILTER)){
				if(GROUP_STRING_TO_STRIP != null && !GROUP_STRING_TO_STRIP.equals("")){
					groupName = groupName.replaceFirst(GROUP_STRING_TO_STRIP, "");
				}
				groups.add(groupName);
			}
		}
		return groups;
	}
	
	public static User syncExistingUser(User user){
		
		LDAPConnection lc = getBindedConnection();
		LDAPEntry ldapEntry;
		try{
			ldapEntry = searchUserinLDAP(lc, user.getUserId());
		}catch(NoSuchUserException nsu){
			Logger.error(CASAuthUtils.class, "Unable to find user in LDAP");
			return null;
		}
		try {
			userAttribute = loadUserAttributes(ldapEntry, user.getUserId());
			syncUserAttributes(userAttribute);
			ArrayList<String> groups = loadUserGroups(liferayUser);
			syncUserGroups (liferayUser, groups);
		} catch (Exception e) {
			Logger.error(CASAuthUtils.class, "Unable to sync user " + user.getUserId() , e);
		}
		
		return user;
	}
	
	public static void syncUserGroups (User liferayUser, ArrayList<String> groups) throws Exception{
		
		if(groups != null){
			List<Role> roles = roleAPI.loadRolesForUser(liferayUser.getUserId(), false);
			for (Role role : roles) {
				roleAPI.removeRoleFromUser(role, liferayUser);
			}
			for (String group : groups) {
				Role r = null; 
				try{
					r = roleAPI.loadRoleByKey(group);
				}catch (Exception e) {
					Logger.debug(CASAuthUtils.class, "Role doesn't exist in dotCMS :" + e.getMessage(), e);
					continue;
				}
				if(r != null && UtilMethods.isSet(r.getId())){
					roleAPI.addRoleToUser(r, liferayUser);
				}else{
					Logger.debug(CASAuthUtils.class, "Unable to add user to role because it doesn exist");
				}
			}
			//Will create the user role if it doesn't exist
			Role role = roleAPI.getUserRole(liferayUser);
			boolean hasRole = roleAPI.doesUserHaveRole(liferayUser, role);
			if(!hasRole){
				roleAPI.delete(role);
				role = roleAPI.getUserRole(liferayUser);
			}
			
			try{
	    		roleAPI.addRoleToUser(roleAPI.loadRoleByKey(LDAP_USER_ROLE), liferayUser);
	    	}catch(Exception ex){
	    		Logger.error(CASAuthUtils.class, "Unable to add user " + liferayUser.getUserId() + " to LDAP User role", ex);
	    	}
		}
	}

	public static void setUserValuesOnSession(User user, HttpServletRequest request, HttpServletResponse response, boolean rememberMe){
		if(LicenseUtil.getLevel()< LicenseLevel.PROFESSIONAL.level) return;
		HttpSession ses = request.getSession();

        // session stuff
        ses.setAttribute(WebKeys.CMS_USER, user);

        //set personalization stuff on session
	}
	
	private static LDAPConnection getBindedConnection() throws DotRuntimeException{
		if(SECURITY_AUTHENTICATION.equalsIgnoreCase("SSL")){
			System.setProperty("javax.net.ssl.trustStore", SECURITY_KEYSTORE_PATH);
			Logger.debug(CASAuthUtils.class, "The trust store is " + System.getProperty("javax.net.ssl.trustStore"));
			 LDAPSocketFactory ssf = new LDAPJSSESecureSocketFactory();

           // Set the socket factory as the default for all future connections
            LDAPConnection.setSocketFactory(ssf);
		 }

		 LDAPConnection lc = new LDAPConnection();
		// connect to the server

	    try{
        lc.connect( HOST, Integer.valueOf(PORT) );
	    }catch (Exception e) {
	    	Logger.error(CASAuthUtils.class,e.getMessage(),e);
		}

		LDAPResponseQueue queue = null;
		LDAPResponse rsp = null;
		try{
			queue = lc.bind(LDAPConnection.LDAP_V3,USERID,PASSWORD.getBytes(),(LDAPResponseQueue)null);

			rsp = (LDAPResponse)queue.getResponse();
		}catch(Exception ex){
			Logger.error(BaseAuthenticator.class,ex.getMessage(),ex);
		}

		int rc = rsp.getResultCode();

		String msg = rsp.getErrorMessage();

		if (rc == LDAPException.SUCCESS){
			Logger.debug(CASAuthUtils.class,"LDAP connection is now bound");
			return lc;
		}else{
			throw new DotRuntimeException("Unable to bind to ldap " + msg);
		}
	}
	
	private static LDAPEntry searchUserinLDAP(LDAPConnection lc,String username)throws NoSuchUserException{
		if (username==null || username.equalsIgnoreCase("")) {
			Logger.info(CASAuthUtils.class, "A username is required");
			return null;
		}
		LDAPSearchResults searchResults = null;
		String dn = "";
		try{
			searchResults = lc.search(DOMAINLOOKUP,LDAPConnection.SCOPE_SUB, USER_ATTRIB + "=" + username, null, false);
		}catch(Exception ex){
			Logger.error(CASAuthUtils.class, "Unable to search for username " + username + " : ",ex);
		}
		if(!searchResults.hasMore()){
			Logger.info(CASAuthUtils.class, "Cannot find username: " + username);
			throw new NoSuchUserException();
		}

		while (searchResults.hasMore()) {

			LDAPEntry ldapEntry = null;

			try{
				ldapEntry = searchResults.next();
				dn = ldapEntry.getDN();
			}catch(Exception e){
				Logger.error(CASAuthUtils.class, "Error while trying to bind user " + username + " : ", e);
				throw new NoSuchUserException("Error. User not found in LDAP.");
			}

			if (UtilMethods.isSet(dn)){
				//Logger.info(CASAuthUtils.class, "User " + username + " was found on LDAP");
				return ldapEntry;
			}else{
				Logger.info(CASAuthUtils.class," User " + username + " was not found on LDAP");
				return null;
			}
		}
		return null;
	}
    
    private static UserAttribute loadUserAttributes(LDAPEntry ldapEntry, String userId){
    	UserAttribute ua = new UserAttribute();
		LDAPAttribute la;
		if(UtilMethods.isSet(FIRST_NAME_ATTRIB)){
			la = ldapEntry.getAttribute(FIRST_NAME_ATTRIB);
			if(la != null)
				ua.setFirstName(la.getStringValue());
		}

		if(UtilMethods.isSet(MIDDLE_NAME_ATTRIB)){
			la = ldapEntry.getAttribute(MIDDLE_NAME_ATTRIB);
			if(la != null)
				ua.setMiddleName(la.getStringValue());
		}
		
		if(UtilMethods.isSet(NICKNAME_ATTRIB)){
			la = ldapEntry.getAttribute(NICKNAME_ATTRIB);
			if(la != null)
				ua.setNickName(la.getStringValue());
		}
		
		if(UtilMethods.isSet(LAST_NAME_ATTRIB)){
			la = ldapEntry.getAttribute(LAST_NAME_ATTRIB);
			if(la != null)
				ua.setLastName(la.getStringValue());
		}
		
		if(UtilMethods.isSet(EMAIL_ATTRIB)){
			ua.setEmailAddress(ldapEntry.getAttribute(EMAIL_ATTRIB)== null ? userId + "@changeme.com" : ldapEntry.getAttribute(EMAIL_ATTRIB).getStringValue());
		}
		if(UtilMethods.isSet(GENDER_ATTRIB)){
			la = ldapEntry.getAttribute(GENDER_ATTRIB);
			if(la != null)
				ua.setMale(la.toString().equalsIgnoreCase("true"));
		}
		
		return ua;
    }
    
    private static boolean syncUserAttributes(UserAttribute ua){
		if(UtilMethods.isSet(FIRST_NAME_ATTRIB)){
			String firstName = userAttribute.getFirstName() == null ? "" : userAttribute.getFirstName();
			liferayUser.setFirstName(firstName);
		}
		if(UtilMethods.isSet(MIDDLE_NAME_ATTRIB)){
			String middleName = userAttribute.getMiddleName()== null ? "" : userAttribute.getMiddleName();
			liferayUser.setMiddleName(middleName);
		}
		if(UtilMethods.isSet(LAST_NAME_ATTRIB)){
			String lastName = userAttribute.getLastName()== null ? "" : userAttribute.getLastName();
			liferayUser.setLastName(lastName);
		}
		if(UtilMethods.isSet(NICKNAME_ATTRIB)){
			String nickName = userAttribute.getNickName()== null ? "" : userAttribute.getNickName();
			liferayUser.setNickName(nickName);
		}
		if(UtilMethods.isSet(GENDER_ATTRIB)){
			liferayUser.setMale(userAttribute.isMale());
		}
		if(UtilMethods.isSet(EMAIL_ATTRIB)){
			liferayUser.setEmailAddress(userAttribute.getEmailAddress().toLowerCase());
		}
				

		
		try {
			userAPI.save(liferayUser,APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(CASAuthUtils.class, "Unable to sync Attributes for user " + liferayUser.getUserId() , e);
			return false;
		}
		
    	return true;
    }
   

}
