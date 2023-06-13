package com.dotcms.enterprise;

import com.dotcms.repackage.com.novell.ldap.LDAPConnection;
import com.dotcms.repackage.com.novell.ldap.LDAPException;
import com.dotcms.repackage.com.novell.ldap.LDAPJSSESecureSocketFactory;
import com.dotcms.repackage.com.novell.ldap.LDAPResponse;
import com.dotcms.repackage.com.novell.ldap.LDAPResponseQueue;
import com.dotcms.repackage.com.novell.ldap.LDAPSocketFactory;
import com.dotmarketing.auth.model.UserAttribute;
import com.dotmarketing.beans.UserProxy;
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
import org.apache.commons.beanutils.BeanUtils;

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
				String passToSync;
				if(!syncUser(password)){
					throw new Exception("Unable to sync user");
				}
			}catch(com.dotmarketing.business.NoSuchUserException nsne){
                Logger.debug(this, "creating the user on liferay");
                try
                {
                	migrateUser(username,companyId,isUserId);
                	syncUser(password);
                }catch(Exception ex){ 
                	Logger.error(this, "Error creating the user on liferay: " + ex.getMessage(), ex); 
            	}
			}catch(Exception ex){
				Logger.error(this, "Error authenticating User : ", ex);
				return FAILURE;
			}
			Logger.debug(BaseAuthenticator.class, "Sync directory service --> CMS Groups");
			try{
				syncUserAttributes(companyId);
			}catch(Exception e){
				Logger.error(BaseAuthenticator.class, "Unable to sync user attributes : ", e);
			}
			if(IS_BUILD_GROUPS){
				try{
					syncUserGroups();
				}catch(Exception e){
					Logger.error(BaseAuthenticator.class, "Unable to sync groups : ", e);
				}
			}
            Logger.debug(this, "Auth module login sucess.");
            return SUCCESS;
		}else{
            Logger.debug(this, "Login against liferay fails");
			return FAILURE;
			
		}
	}
	/**
	 * Migrates a user if they do not exist yet in the dotcms.
	 * @param username
	 * @param password
	 * @param encrypterPassword
	 * @param companyId
	 * @param isUserId
	 * @param emailAddress
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
			Logger.debug(BaseAuthenticator.class, "User Migration Successfull");
			return true;
		}catch(Exception ex){
			Logger.error(BaseAuthenticator.class,"Failed to migrate user " + username + ": ",ex);
			return false;
		}
	}
	/**
	 * Syncs groups from directory service to dotcms.
	 * @param user
	 * @param idEmail
	 * @param byUserId
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
    		for (String group : groups) {
    			Role r = null; 
    			try{
    				r = APILocator.getRoleAPI().loadRoleByKey(group);
    			}catch (Exception e) {
					Logger.debug(this, "Role doesn't exist in dotCMS :" + e.getMessage(), e);
					continue;
				}
    			if(r != null && UtilMethods.isSet(r.getId())){
    				APILocator.getRoleAPI().addRoleToUser(r, liferayUser);
    			}else{
    				Logger.debug(BaseAuthenticator.class, "Unable to add user to role because it doesn exist");
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
				
		if(userAttribute.getCustomProperties() != null){
			UserProxy up = new UserProxy();
			BeanUtils.copyProperties(up, userAttribute.getCustomProperties());
			com.dotmarketing.business.APILocator.getUserProxyAPI().saveUserProxy(up,APILocator.getUserAPI().getSystemUser(), false);
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

		 LDAPConnection lc = new LDAPConnection();
		// connect to the server

	    try{
        lc.connect( HOST, Integer.valueOf(PORT) );
	    }catch (Exception e) {
	    	Logger.error(this,e.getMessage(),e);
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
			Logger.debug(this,"LDAP connection is now bound");
			return lc;
		}else{
			throw new DotRuntimeException("Unable to bind to ldap " + msg);
		}
	}
}
