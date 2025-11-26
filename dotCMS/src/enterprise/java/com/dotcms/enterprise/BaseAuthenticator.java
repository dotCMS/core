/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise;

import com.dotcms.repackage.com.novell.ldap.LDAPConnection;
import com.dotcms.repackage.com.novell.ldap.LDAPException;
import com.dotcms.repackage.com.novell.ldap.LDAPJSSESecureSocketFactory;
import com.dotcms.repackage.com.novell.ldap.LDAPResponse;
import com.dotcms.repackage.com.novell.ldap.LDAPResponseQueue;
import com.dotcms.repackage.com.novell.ldap.LDAPSocketFactory;
import com.dotmarketing.auth.model.UserAttribute;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portal.struts.DotCustomLoginPostAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * The BaseAuthicator can be extended to provide authentication to a third-party
 * authentication source. There are 3 methods that the concrete class will have to expose.
 * @author jtesser
 * @version 1.5
 */
public abstract class BaseAuthenticator implements Authenticator {

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

	private UserAttribute userAttribute;
	private User liferayUser;
	private String userPassword;
	private String userLogin;
	private boolean isUserId;

	/**
	 * Return true/false based on user credentials
	 * @param username The username to authenticate
	 * @param password The password for the user to authenticate
	 * @return
	 */
	public abstract boolean authenticate(String username, String password)throws NoSuchUserException;
	/**
	 * Expose this method if you wish to sync groups from the directory service to the dotcms.
	 * If you do not want to sync attributes have the concrete class return this method as null.
	 * @param username the username in the directory service to load attributes for
	 * @return ArrayList of a user's groups.
	 */
	public abstract ArrayList<String> loadGroups(String username, String password);
	/**
	 * Expose this method if you want to sync user attributes to the dotcms.  You can set
	 * first name, middle name, last name nickname, email, gender, birthday, multiple addresses,
	 * and any attribute in the user_proxy table.  If you do not want to sync attributes have the
	 * concrete class return this method as null.
	 * @param username the username in the directory service to load attributes for
	 * @return UserAttribute
	 */
	public abstract UserAttribute loadAttributes(String username, String password);

	protected BaseAuthenticator() {}
	
	public int authenticateByEmailAddress(String companyId, String emailAddress, String password) throws AuthException {
		this.userPassword = password;
		this.userLogin = emailAddress;
		this.isUserId = false;
		return authUser(companyId, emailAddress, password);
	}

	public int authenticateByUserId(String companyId, String userId, String password) throws AuthException {
		this.userPassword = password;
		this.userLogin = userId;
		this.isUserId = true;
		return authUser(companyId, userId, password);
	}

	private int authUser(String companyId, String username, String password){
		if(DotCustomLoginPostAction.FAKE_PASSWORD.equals(password)){
			return FAILURE;
		}
		boolean authenticated = false;
		boolean deleteLiferayUser = false;
		try{
			authenticated = authenticate(username, password);
		}catch (NoSuchUserException nsu) {
			deleteLiferayUser = true;
		}

		if(authenticated){
			userAttribute = loadAttributes(username, password);
			try{
				if(!isUserId){
					liferayUser = APILocator.getUserAPI().loadByUserByEmail(username.toLowerCase(), APILocator.getUserAPI().getSystemUser(), false);
				}else{
					liferayUser = APILocator.getUserAPI().loadUserById(username,APILocator.getUserAPI().getSystemUser(),false);
				}
				if(!syncUser(password)){
					throw new Exception(String.format("User '%s' could not be synched.", username));
				}
			} catch (final com.dotmarketing.business.NoSuchUserException nsne) {
				Logger.debug(this, String.format("User '%s' was not found. Creating it...", username));
                try
                {
                	migrateUser(username,companyId,isUserId);
                	syncUser(password);
                } catch (final Exception ex) {
                    Logger.error(this, String.format("An error occurred when creating user '%s': %s", username, ex
                            .getMessage()), ex);
                }
			} catch (final Exception ex) {
                Logger.error(this, String.format("An error occurred when loading user '%s': %s", username, ex
                        .getMessage()), ex);
                return FAILURE;
			}
			Logger.debug(BaseAuthenticator.class, "Sync directory service --> CMS Groups");
			try{
				syncUserAttributes(companyId);
			} catch (final Exception e) {
                Logger.error(BaseAuthenticator.class, String.format("Attributes for user '%s' could not be synched: " +
                        "%s", username, e.getMessage()), e);
            }
			if(IS_BUILD_GROUPS){
				try{
					syncUserGroups();
				} catch (final Exception e) {
                    Logger.error(BaseAuthenticator.class, String.format("Groups for user '%s' could not be synched: " +
                            "%s", username, e.getMessage()), e);
				}
			}
            Logger.debug(this, "Auth module login successful.");
            return SUCCESS;
		}else{

            Logger.debug(this, String.format("User '%s' could not be authenticated. Trying to log in against " +
                    "Liferay", username));
            try {
				if (isUserId){
					liferayUser = APILocator.getUserAPI().loadUserById(username,APILocator.getUserAPI().getSystemUser(),false);
				}else{
					liferayUser = APILocator.getUserAPI().loadByUserByEmail(username.toLowerCase(), APILocator.getUserAPI().getSystemUser(), false);
				}
				if(deleteLiferayUser && APILocator.getRoleAPI().doesUserHaveRole(liferayUser, APILocator.getRoleAPI().loadRoleByKey(LDAP_USER_ROLE))){
					Logger.info(this, "Deleting user " + liferayUser.getUserId() + " from portal because he is no longer in LDAP");
					APILocator.getUserAPI().delete(liferayUser, APILocator.getUserAPI().getSystemUser(), false);
					return FAILURE;
				}else if(!SYNC_PASSWORD && APILocator.getRoleAPI().doesUserHaveRole(liferayUser, APILocator.getRoleAPI().loadRoleByKey(LDAP_USER_ROLE))){
					Logger.debug(this, "Failed to Auth against LDAP and passwords are not synced so auth will fail");
					return FAILURE;
				}else{
					Logger.debug(this, "Login against liferay succeeded");
					return SUCCESS;
				}
			} catch (final Exception e) {
                Logger.error(this, String.format("Liferay login for user '%s' has failed: %s", username, e.getMessage()), e);

                return FAILURE;
			}
			
		}
	}

	/**
	 * Migrates a user if they do not exist yet in the dotcms.
	 * @param username
	 * @param companyId
	 * @param isUserId
	 * @return true/false if user was successfully migrated
	 * @throws Exception
	 */
	private boolean migrateUser(String username,String companyId,boolean isUserId) throws Exception{
		try{
			liferayUser = APILocator.getUserAPI().createUser(isUserId ? username:null, isUserId ? null:username);
			liferayUser.setActive(true);
			liferayUser.setCompanyId(companyId);
			liferayUser.setPassword(userPassword);			
			liferayUser.setCreateDate(new java.util.Date());
			
			APILocator.getUserAPI().save(liferayUser,APILocator.getUserAPI().getSystemUser(),false);
	    	try{
	    		APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadRoleByKey(LDAP_USER_ROLE), liferayUser);
	    	}catch(Exception ex){
	    		Logger.error(this, "Unable to add user " + liferayUser.getUserId() + " to LDAP User role", ex);
	    	}
			Logger.debug(BaseAuthenticator.class, "User Migration Successful");
			return true;
		} catch (final Exception ex) {
            Logger.error(BaseAuthenticator.class, String.format("User '%s' could not be migrated: %s ", username, ex
                    .getMessage()), ex);
            return false;
		}
	}

	/**
	 * Syncs groups from directory service to dotcms.
     *
	 * @return true/false if all groups was successfully synced
	 * @throws Exception
=	 */
    private boolean syncUserGroups () throws Exception{
    	ArrayList<String> groups = loadGroups(userLogin, userPassword);
    	if(groups != null){
    		List<Role> roles = APILocator.getRoleAPI().loadRolesForUser(liferayUser.getUserId(), false);
    		for (Role role : roles) {
				APILocator.getRoleAPI().removeRoleFromUser(role, liferayUser);
			}

    		boolean backedRoleAdded = false;

    		for (String group : groups) {
    			Role role;
    			try{
    				role = APILocator.getRoleAPI().loadRoleByKey(group);
    			} catch (final Exception e) {
                    Logger.debug(this, String.format("Role '%s' could not be found: %s", group, e.getMessage()), e);
                    continue;
				}
    			if(role != null && UtilMethods.isSet(role.getId())){
    				APILocator.getRoleAPI().addRoleToUser(role, liferayUser);

    				if(!backedRoleAdded && UtilMethods.isSet(APILocator.getLayoutAPI().loadLayoutsForRole(role))) {
						APILocator.getRoleAPI().addRoleToUser(
								APILocator.getRoleAPI().loadBackEndUserRole(), liferayUser);
						backedRoleAdded = true;
					}

    			}else{
                    Logger.debug(BaseAuthenticator.class, String.format("Unable to add user to role '%s' because it " +
                            "doesn't exist", group));
                }
			}
    		//Will create the userrole if it doesn't exist
    		Role role = APILocator.getRoleAPI().getUserRole(liferayUser);
    		boolean hasRole = APILocator.getRoleAPI().doesUserHaveRole(liferayUser, role);
    		if(!hasRole){
    			APILocator.getRoleAPI().delete(role);
    			role = APILocator.getRoleAPI().getUserRole(liferayUser);
    		}
    		
    		try{
	    		APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadRoleByKey(LDAP_USER_ROLE), liferayUser);
	    	}catch(Exception ex){
	    		Logger.error(this, "Unable to add user " + liferayUser.getUserId() + " to LDAP User role", ex);
	    	}
    	}
    	return true;
    }

    /**
     * Syncs the user's password to liferay
     * @param password users password to sync to liferay table
     * @return
     */
    private boolean syncUser(String password)throws Exception{
        // Use new password hash method
        liferayUser.setPassword(PasswordFactoryProxy.generateHash(password));

    	APILocator.getUserAPI().save(liferayUser,APILocator.getUserAPI().getSystemUser(),false);
    	boolean userHasLDAPRole = false;
    	List<Role> roles = APILocator.getRoleAPI().loadRolesForUser(liferayUser.getUserId(), false);
		for (Role role : roles) {
			if(role.getRoleKey() != null && role.getRoleKey().equals(LDAP_USER_ROLE)){
				userHasLDAPRole = true;
			}
		}
		if(!userHasLDAPRole){
	    	try{
	    		APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadRoleByKey(LDAP_USER_ROLE), liferayUser);
	    	}catch(Exception ex){
	    		Logger.error(this, "Unable to add user " + liferayUser.getUserId() + " to LDAP User role", ex);
	    	}
		}
    	return true;
    }

    /**
     * Syncs user attributes from directory service to dotcms.  Will sync custom properties to
     * user_proxy if it is not null.
     * @return true/false on successful sync of of user attributes
     */
    private boolean syncUserAttributes(String companyId) throws Exception{
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
			if(!isUserId){
				liferayUser.setEmailAddress(userLogin.toLowerCase());
			}else{
				liferayUser.setEmailAddress(userAttribute.getEmailAddress().toLowerCase());
			}
		}
				

		
		APILocator.getUserAPI().save(liferayUser,APILocator.getUserAPI().getSystemUser(),false);
		
    	return true;
    }

	protected LDAPConnection getBindedConnection() throws DotRuntimeException{
		if(SECURITY_AUTHENTICATION.equalsIgnoreCase("SSL")){
			System.setProperty("javax.net.ssl.trustStore", SECURITY_KEYSTORE_PATH);
			Logger.debug(this, "The trust store is " + System.getProperty("javax.net.ssl.trustStore"));
			 LDAPSocketFactory ssf = new LDAPJSSESecureSocketFactory();

           // Set the socket factory as the default for all future connections
            LDAPConnection.setSocketFactory(ssf);
		 }

		 LDAPConnection ldapConnection = new LDAPConnection();
		// connect to the server

	    try{
            ldapConnection.connect( HOST, Integer.valueOf(PORT) );
	    } catch (final Exception e) {
            Logger.error(this, String.format("An error occurred when connecting to LDAP host '%s' and port '%s': %s",
                    HOST, PORT, e.getMessage()), e);
        }

		LDAPResponseQueue queue;
		LDAPResponse rsp = null;
		try{
			queue = ldapConnection.bind(LDAPConnection.LDAP_V3,USERID,PASSWORD.getBytes(),(LDAPResponseQueue)null);

			rsp = (LDAPResponse)queue.getResponse();
		} catch (final Exception ex) {
            Logger.error(BaseAuthenticator.class, String.format("An error occurred when binding to LDAP host '%s' and" +
                    " port '%s': %s", HOST, PORT, ex.getMessage()), ex);
        }

		int resultCode = rsp.getResultCode();

		String msg = rsp.getErrorMessage();

		if (resultCode == LDAPException.SUCCESS){
			Logger.debug(this,"LDAP connection is now bound");
			return ldapConnection;
		}else{
            throw new DotRuntimeException(String.format("Connection to LDAP host '%s' and port '%s' could not be " +
                    "established: %s", HOST, PORT, msg));
        }
	}

}
