package com.dotcms.saml;

import com.dotcms.plugin.saml.v3.service.OpenSamlAuthenticationServiceImpl;
import com.dotcms.plugin.saml.v3.service.SamlAuthenticationService;

/**
 * Encapsulates constants for the dot SAML SP
 *
 * @author jsanca
 */
// Migrated
public class DotSamlConstants {
	public static final char ARRAY_SEPARATOR_CHAR = ',';
	public static final String HTTP_SCHEMA = "http://";
	public static final String HTTPS_SCHEMA = "https://";
	public static final String HTTPS_SCHEMA_PREFIX = "https";
	public static final String ASSERTION_CONSUMER_ENDPOINT_DOTSAML3SP = "/dotsaml/login";
	public static final String LOGOUT_SERVICE_ENDPOINT_DOTSAML3SP = "/dotsaml/logout";
	public static final String RESPONSE_AND_ASSERTION = "responseandassertion";
	public static final String RESPONSE = "response";
	public static final String ASSERTION = "assertion";
	public static final String SAML_USER_ID = "SAMLUserId";
	public static final String DEFAULT_LOGIN_PATH = "/dotAdmin";

	public static final String SAML_NAME_ID_SESSION_ATTR = "SAML_NAME_ID";

	public static final String SAML_ART_PARAM_KEY = "SAMLart";

	/**
	 * By default we use the {@link OpenSamlAuthenticationServiceImpl}, however
	 * if you want to create or customize your own it is possible by
	 * implementing {@link SamlAuthenticationService} or just extending
	 * {@link OpenSamlAuthenticationServiceImpl} If you need to override it,
	 * just set the classname with this property
	 */
	public static final String DOT_SAML_AUTHENTICATION_SERVICE_CLASS_NAME = "authentication.service.classname";

	/**
	 * Optional key to configure the strategy to sync the roles from IDP to
	 * DOTCMS Remove user from all roles, add to roles from IdP & saml_user_role
	 * (if set) DOTCMS_SAML_BUILD_ROLES_ALL_VALUE = " all"; Remove user from all
	 * roles, add to roles from IdP DOTCMS_SAML_BUILD_ROLES_IDP_VALUE = "idp";
	 * Remove user from all roles, add to roles from saml_user_role (if set).
	 * Ignore roles from IdP. DOTCMS_SAML_BUILD_ROLES_STATIC_ONLY_VALUE =
	 * "staticonly; Do not alter existing user roles, add to roles from
	 * saml_user_role (if set). Ignore roles from IdP.
	 * DOTCMS_SAML_BUILD_ROLES_STATIC_ADD_VALUE = "staticadd;
	 * DOTCMS_SAML_BUILD_ROLES_NONE_VALUE Do not alter user roles in any way
	 */

	public static final String DOTCMS_SAML_BUILD_ROLES_ALL_VALUE = "all";

	public static final String DOTCMS_SAML_BUILD_ROLES_IDP_VALUE = "idp";

	public static final String DOTCMS_SAML_BUILD_ROLES_NONE_VALUE = "none";

	public static final String DOTCMS_SAML_BUILD_ROLES_STATIC_ADD_VALUE = "staticadd";

	public static final String DOTCMS_SAML_BUILD_ROLES_STATIC_ONLY_VALUE = "staticonly";

	/**
	 * Default value for the metadata protocol see
	 * {@link DotSamlConstants}.DOT_SAML_IDP_METADATA_PROTOCOL
	 */
	public static final String DOT_SAML_IDP_METADATA_PROTOCOL_DEFAULT_VALUE = "urn:oasis:names:tc:SAML:2.0:protocol";

	/**
	 * default include path
	 */
	public static final String DOT_SAML_INCLUDE_PATH_DEFAULT_VALUES = "^" + ASSERTION_CONSUMER_ENDPOINT_DOTSAML3SP
			+ "$," + "^/dotCMS/login.*$," + "^/html/portal/login.*$," + "^/c/public/login.*$,"
			+ "^/c/portal_public/login.*$," + "^/c/portal/logout.*$," + "^/dotCMS/logout.*$,"
			+ "^/application/login/login.*$," + "^/dotAdmin.*$";

	/**
	 * default logout path values
	 */
	public static final String DOT_SAML_LOGOUT_PATH_DEFAULT_VALUES = "/api/v1/logout,/c/portal/logout,/dotCMS/logout,/dotsaml/request/logout";

	/**
	 * Default SAML User role
	 */
	public static final String DOTCMS_SAML_USER_ROLE = "SAML User";
	
}
