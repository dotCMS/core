package com.dotcms.saml.service.external;


/**
 * Encapsulates constants for the dot SAML SP
 * Such as the StringUtils or Webkeys but in SAML
 * Many of the default values are here
 * @author jsanca
 */

public class SamlConstants {
	public static final char ARRAY_SEPARATOR_CHAR = ',';
	public static final String HTTP_SCHEMA = "http://";
	public static final String HTTPS_SCHEMA = "https://";
	public static final String HTTPS_SCHEMA_PREFIX = "https";
	public static final String ASSERTION_CONSUMER_ENDPOINT_DOTSAML3SP = "/dotsaml/login";
	public static final String LOGOUT_SERVICE_ENDPOINT_DOTSAML3SP = "/dotsaml/logout";
	public static final String RESPONSE_AND_ASSERTION = "responseandassertion";
	public static final String RESPONSE = "response";
	public static final String ASSERTION = "assertion";

	/**
	 * Optional IdP property. When "true", responses without InResponseTo (IdP-initiated SSO) are accepted.
	 * By default only responses to an authentication request started from the same browser are accepted.
	 */
	public static final String ALLOW_UNSOLICITED_RESPONSES = "allow.unsolicited.responses";

	/**
	 * Optional IdP property. Seconds an outstanding authentication request stays valid while the user
	 * authenticates at the IdP.
	 */
	public static final String AUTHN_REQUEST_MAX_AGE = "authn.request.max.age";
	public static final int AUTHN_REQUEST_MAX_AGE_DEFAULT_VALUE = 900;

	/**
	 * Optional IdP property. When "true", RSA-SHA1 signatures and SHA-1 digests are accepted (with a warning)
	 * for IdPs that can not sign with SHA-256 or stronger yet.
	 */
	public static final String ALLOW_SHA1_SIGNATURES = "allow.sha1.signatures";

	public static final String SAML_USER_ID = "SAMLUserId";
	public static final String DEFAULT_LOGIN_PATH = "/dotAdmin";

	public static final String SAML_NAME_ID_SESSION_ATTR = "SAML_NAME_ID";

	public static final String SAML_ART_PARAM_KEY = "SAMLart";

	/**
	 * Default value for the metadata protocol see
	 * {@link SamlConstants}.DOT_SAML_IDP_METADATA_PROTOCOL
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
	public static final String DOT_SAML_LOGOUT_PATH_DEFAULT_VALUES = "/c/portal/logout,/dotCMS/logout,/dotsaml/request/logout,/dotAdmin/logout";

	/**
	 * Default SAML User role
	 */
	public static final String DOTCMS_SAML_USER_ROLE = "SAML User";

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
	
}
