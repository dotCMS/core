/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

/**
 *
 */
package com.dotcms.enterprise;

import com.dotcms.repackage.com.novell.ldap.LDAPAttribute;
import com.dotcms.repackage.com.novell.ldap.LDAPConnection;
import com.dotcms.repackage.com.novell.ldap.LDAPEntry;
import com.dotcms.repackage.com.novell.ldap.LDAPException;
import com.dotcms.repackage.com.novell.ldap.LDAPResponse;
import com.dotcms.repackage.com.novell.ldap.LDAPResponseQueue;
import com.dotcms.repackage.com.novell.ldap.LDAPSearchResults;
import com.dotmarketing.auth.model.UserAttribute;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.NoSuchUserException;

import java.util.ArrayList;
import java.util.Enumeration;

/**
 * LDAPImpl class will authenticate against an Open LDAP server, load groups, and load basic attributes into the dotcms.
 * You are expected to config the following properties in the portal-ext.properties to use this class.
 * auth.impl.ldap.initial.context.factory=com.sun.jndi.ldap.LdapCtxFactory
 * auth.impl.ldap.security.authentication=none
 * auth.impl.ldap.host=10.1.1.22
 * auth.impl.ldap.port=389
 * auth.impl.ldap.userid=Administrator
 * auth.impl.ldap.password=password
 * auth.impl.ldap.domainlookup=dc=liferay,dc=com
 * auth.impl.build.groups=true
 * auth.impl.ldap.build.group.name.filter=regex with selection
 * auth.impl.ldap.attrib.group=LDAP group attribute name
 * auth.impl.lpap.attrib.user=uid - LDAP user attribute name

 * The following properties can be defined in the portal-ext.properties for each attribute to be used in DotCMS
 * auth.impl.ldap.attrib.firstName=givenName
 * auth.impl.ldap.attrib.middleName=
 * auth.impl.ldap.attrib.lastName=sn
 * auth.impl.ldap.attrib.nickName=
 * auth.impl.ldap.attrib.email=mail
 * auth.impl.ldap.attrib.gender=
 * auth.impl.ldap.attrib.birthday=
 *
 * @author jtesser
 *
 */

public class LDAPImpl extends BaseAuthenticator {

	protected LDAPImpl() {}

	@Override
	public boolean authenticate(final String username, final String password)throws NoSuchUserException {
		   LDAPConnection lc;
		   try{
			   lc = getBindedConnection();
		   } catch (final DotRuntimeException dre) {
			   Logger.error(this, String.format("Error when Binding Connection: %s", dre.getMessage()), dre);
			   return false;
		   }
		   if(bindUser(lc, username, password) != null){
			   return true;
		   }else{
               Logger.debug(this, String.format("User '%s' was unable to authenticate", username));
               return false;
		   }
	}

	@Override
	public UserAttribute loadAttributes(String username, String pass) {
		LDAPConnection lc = getBindedConnection();
		LDAPEntry ldapEntry;
		try{
			ldapEntry = bindUser(lc, username, pass);
		}catch(NoSuchUserException nsu){
			Logger.error(this, "Error Loading User Attributes", nsu);
			return null;
		}
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
			ua.setEmailAddress(ldapEntry.getAttribute(EMAIL_ATTRIB)== null ? username + "@changeme.com" : ldapEntry.getAttribute(EMAIL_ATTRIB).getStringValue());
		}
		if(UtilMethods.isSet(GENDER_ATTRIB)){
			la = ldapEntry.getAttribute(GENDER_ATTRIB);
			if(la != null)
				ua.setMale(la.toString().equalsIgnoreCase("true"));
		}
		return ua;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.auth.BaseAuthenticator#loadGroups(java.lang.String, java.lang.String)
	 *
	 * Accepts: An LDAP username and password
	 * Requires:  LDAP should be using the mail attribute to store e-mail addresses
	 *			    and should store the group information in the isMemberOf attribute.
	 *			  The following entries in portal-ext.properties
	 *			  	auth.impl.build.groups=true
	 * 			  	auth.impl.ldap.build.group.name.filter=regex with selection.
	 * Outputs: A string list of group names that the given user is a member of.
	 */
	@Override
	public ArrayList<String> loadGroups(String username, String password) {
		ArrayList<String> groups = new ArrayList<>();
		LDAPConnection lc = getBindedConnection();
		LDAPEntry ldapEntry;
		try{
			ldapEntry = bindUser(lc, username, password);
		}catch(NoSuchUserException nsu){
			Logger.error(this, "Unable to find user in LDAP");
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

	private LDAPEntry bindUser(LDAPConnection ldapConnection,String username, String pass)throws NoSuchUserException{
		if (pass==null || pass.equalsIgnoreCase("")) {
			Logger.info(this, "No password supplied for username: " + username);
			return null;
		}
		LDAPSearchResults searchResults = null;
		LDAPResponseQueue queue;
		LDAPResponse rsp;
		try{
			searchResults = ldapConnection.search(DOMAINLOOKUP,LDAPConnection.SCOPE_SUB, USER_ATTRIB + "=" + username, null, false);
        } catch (final Exception ex) {
            Logger.error(this, String.format("An error occurred when searching for user '%s': %s", username, ex
                    .getMessage()), ex);
        }
        if(!searchResults.hasMore()){
		    final String errorMsg = String.format("User '%s' was not found", username);
			Logger.info(this, errorMsg);
			throw new NoSuchUserException(errorMsg);
		}

		while (searchResults.hasMore()) {

			LDAPEntry ldapEntry;

			try{
				ldapEntry = searchResults.next();

				String dn = ldapEntry.getDN();

				queue = ldapConnection.bind(LDAPConnection.LDAP_V3,dn,pass.getBytes(),(LDAPResponseQueue)null);

				rsp = (LDAPResponse)queue.getResponse();
			} catch (final Exception e) {
                final String errorMsg = String.format("An error occurred while trying to bind user '%s': %s",
                        username, e.getMessage());
                Logger.error(this, errorMsg);
				throw new NoSuchUserException(errorMsg);
			}

	           // get the return code and the message from the response

			int resultCode = rsp.getResultCode();

			String msg = rsp.getErrorMessage();

			if (resultCode == LDAPException.SUCCESS){
				Logger.info(this, "User " + username + " logged in");
				return ldapEntry;
			}else{
                Logger.error(this, String.format("User '%s' failed to log in: %s [%s]", username, msg, resultCode));
                return null;
			}

		}
		return null;
	}

}
