package com.dotcms.saml;

/**
 * Encapsulates the Dot Saml names for configuration
 * @author jsanca
 */
public enum SamlName {

	/**
	 * Tell the system to merge the additional attributes from the IDP or override (default behavior)
	 */
	DOTCMS_MERGE_ADDITIONAL_ATTRIBUTES("merge.additional.attributes"),

	/**
	 * To set enable or disable an idp configuration
	 */
	DOT_SAML_ENABLE( "enable"),


	/**
	 * Id for the Issuer, it is the SP identifier on the IdP
	 */
	DOT_SAML_SERVICE_PROVIDER_ISSUER_URL("sPIssuerURL"),

	/**
	 * Name for our SP
	 */
	DOT_SAML_IDENTITY_PROVIDER_NAME("idpName"),

	/**
	 * SP Endpoint hostname
	 */
	DOT_SAML_SERVICE_PROVIDER_HOST_NAME("sPEndpointHostname"),

	/**
	 * Validation Type: assertion only, response only or both
	 */
	DOT_SAML_SIGNATURE_VALIDATION_TYPE("signatureValidationType"),

	/**
	 * XML witht the identity provider metadata
	 */
	DOT_SAML_IDENTITY_PROVIDER_METADATA_FILE("idPMetadataFile"),

	/**
	 * Private key
	 */
	DOT_SAML_PRIVATE_KEY_FILE("privateKey"),

	/**
	 * Public Cert
	 */
	DOT_SAML_PUBLIC_CERT_FILE("publicCert"),

	////

	/**
	 * By default we do not filter anything, but if there is some special cases
	 * (url's) you want to avoid the authentication check, add here the values
	 * comma separated.
	 */
	DOT_SAML_ACCESS_FILTER_VALUES("access.filter.values"),
	
	/**
	 * By default dotCMS uses:
	 * org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration#MINIMUM,
	 * but you can set any different you setting the value (non-case sensitive)
	 * For instance: <code>
	 * authn.comparisontype=BETTER
	 * </code> Will use org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration#BETTER
	 * instead of MINIMUN.
	 */
	DOTCMS_SAML_AUTHN_COMPARISON_TYPE( "authn.comparisontype"),
	
	/**
	 * By default dotCMS uses: org.opensaml.saml.saml2.core.AuthnContext#PASSWORD_AUTHN_CTX, but
	 * you can override it just adding the context class ref you want.
	 */
	DOTCMS_SAML_AUTHN_CONTEXT_CLASS_REF("authn.context.class.ref"),
	
	/**
	 * By default we use: urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect
	 * BindingType#REDIRECT But if you want to use a diff mechanism from
	 * the Single Sign On Service (see SingleSignOnService tag on the
	 * idp-metadata) please override it
	 *
	 */
	DOTCMS_SAML_BINDING_TYPE("bindingtype"),
	
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
	DOTCMS_SAML_BUILD_ROLES("build.roles"),
	
	/**
	 * By default dotcms use: 1000, but you can override it just adding the new
	 * time you want.
	 */
	DOT_SAML_CLOCK_SKEW("clock.skew"),
	
	/**
	 * By default dotcms use: "mail", but you can override it just adding the
	 * mail attribute name you want. "mail" will be the expected field name from
	 * the Response coming from the OpenSaml post call.
	 */
	DOT_SAML_EMAIL_ATTRIBUTE("attribute.email.name"),
	
	/**
	 * Boolean value to allow to build a dummy email based on the NameID from
	 * the Idp when the email attribute from the IDP is not present. True will
	 * apply the email generation, false will throw 401 error.
	 */
	DOT_SAML_EMAIL_ATTRIBUTE_ALLOW_NULL("attribute.email.allownull"),
	
	/**
	 * By default dotcms use: "givenName", but you can override it just adding
	 * the first name attribute name you want. "givenName" will be the expected
	 * field name from the Response comming from the OpenSaml post call.
	 */
	DOT_SAML_FIRSTNAME_ATTRIBUTE("attribute.firstname.name"),
	
	/**
	 * Key for host field configuration (see
	 * SamlConstants#DOT_SAML_FIRSTNAME_ATTRIBUTE_NULL_VALUE
	 * If the first name attribute is null, this value will be set instead
	 */
	DOT_SAML_FIRSTNAME_ATTRIBUTE_NULL_VALUE("attribute.firstname.nullvalue"),
	
	/**
	 * By default the app will try to logout on any site, however you can
	 * override this property per site in order to avoid the plugin to handle
	 * the logout.
	 */
	DOTCMS_SAML_IS_LOGOUT_NEED("islogoutneed"),
	
	/**
	 * By default dotcms use: "sn", but you can override it just adding the last
	 * name attribute name you want. "sn" will be the expected field name from
	 * the Response comming from the OpenSaml post call.
	 */
	DOT_SAML_LASTNAME_ATTRIBUTE("attribute.lastname.name"),
	
	/**
	 * Key for host field configuration (see
	 * SamlConstants#DOT_SAML_FIRSTNAME_ATTRIBUTE_NULL_VALUE
	 * If the last name attribute is null, this value will be set instead
	 */
	DOT_SAML_LASTNAME_ATTRIBUTE_NULL_VALUE("attribute.lastname.nullvalue"),
	
	/**
	 * This is the logout service endpoint url, which means the url where to be
	 * redirected when the user gets log out. You can set it for instance to
	 * http://[domain]/c in order to get back to the page.
	 */
	DOT_SAML_LOGOUT_SERVICE_ENDPOINT_URL("logout.service.endpoint.url"),
	
	/**
	 * DefaultMetaDescriptorServiceImpl is what we use to
	 * parse the idp metadata XML file however if you have you own
	 * implementation of MetaDescriptorService you can override it.
	 */
	DOT_SAML_IDP_METADATA_PARSER_CLASS_NAME("idp.metadata.parser.classname"),
	
	/**
	 * By default dot cms use
	 * SamlConstants#DOT_SAML_IDP_METADATA_PROTOCOL_DEFAULT_VALUE, in
	 * case you need to use a different feel free to override this property.
	 */
	DOT_SAML_IDP_METADATA_PROTOCOL("idp.metadata.protocol"),
	
	/**
	 * By default true, you can override as a false if your assertions are
	 * returned non-encrypted.
	 */
	DOTCMS_SAML_IS_ASSERTION_ENCRYPTED("isassertion.encrypted"),
	
	/**
	 * By default dotcms use: 2000, but you can override it just adding the new
	 * time you want.
	 */
	DOT_SAML_MESSAGE_LIFE_TIME("message.life.time"),
	
	/**
	 * By default we use the implementation
	 * handler.HttpPostAssertionResolverHandlerImpl which is in charge
	 * of resolve the assertion using the SOAP artifact resolver based on the
	 * artifact id pass by the request. If you want a different implementation
	 * please override with the class here.
	 */
	DOTCMS_SAML_ASSERTION_RESOLVER_HANDLER_CLASS_NAME("assertion.resolver.handler.classname"),
	
	/**
	 * By default false, you can override as a true if you want to force the
	 * authentication.
	 */
	DOTCMS_SAML_FORCE_AUTHN("force.authn"),
	
	/**
	 * In case you need a custom credentials for the ID Provider (DotCMS)
	 * overrides the implementation class on the configuration properties.
	 */
	DOT_SAML_ID_PROVIDER_CUSTOM_CREDENTIAL_PROVIDER_CLASSNAME("id.provider.custom.credential.provider.classname"),
	
	/**
	 * If you have already set a idp-metadata, this value will be taken from it,
	 * otherwise you have to set it on the dotCMS properties. If the value is
	 * not present will got an exception on runtime. This value is the Redirect
	 * SLO (Logout) url (usually Shibboleth), which is the one to be redirect
	 * when the user does logout on dotCMS.
	 */
	DOTCMS_SAML_IDENTITY_PROVIDER_DESTINATION_SLO_URL("identity.provider.destinationslo.url"),
	
	/**
	 * If you have already set a idp-metadata, this value will be taken from it,
	 * otherwise you have to set it on the dotCMS properties. If the value is
	 * not present will got an exception on runtime. This value is the Redirect
	 * SSO url (usually Shibboleth), which is the one to be redirect when the
	 * user is not logged on dotCMS.
	 */
	DOTCMS_SAML_IDENTITY_PROVIDER_DESTINATION_SSO_URL("identity.provider.destinationsso.url"),
	
	/**
	 * By default we include /c and /admin, if you need to add more into the
	 * saml filter you can include the values comma separated.
	 */
	DOT_SAML_INCLUDE_PATH_VALUES("include.path.values"),
	
	/**
	 * This is an array comma separated, if this array is set. Any role from
	 * SAML that does not match with the list of include roles pattern, will be
	 * filtered.
	 */
	DOTCMS_SAML_INCLUDE_ROLES_PATTERN("include.roles.pattern"),
	
	/**
	 * By default we include "/c/portal/logout,/dotCMS/logout", if you need to
	 * add more into the saml more path you can include the values comma
	 * separated.
	 */
	DOT_SAML_LOGOUT_PATH_VALUES("logout.path.values"),
	
	/**
	 * SAML allows several formats, such as Kerberos, email, Windows Domain
	 * Qualified Name, etc. By default dotcms use:
	 * org.opensaml.saml.saml2.core.NameIDType#TRANSIENT. See More on
	 * org.opensaml.saml.saml2.core.NameIDType
	 */
	DOTCMS_SAML_NAME_ID_POLICY_FORMAT("nameidpolicy.format"),
	
	/**
	 * Optional key. Role to be assigned to a logged user besides the default
	 * SAML User
	 */
	DOTCMS_SAML_OPTIONAL_USER_ROLE("role.extra"),

	/**
	 * By default dotcms will allows the user synchronization, this means if the user does not exists on their database the user will be added to their storage, roles, etc.
	 * In case you do not want any synchronization set this to false.
	 *
	 */
	DOT_SAML_ALLOW_USER_SYNCHRONIZATION("allow.user.synchronization"),


	/**
	 * If you want to allow to create an user that does not exists on the IdP,
	 * set this to true, otherwise false. By default it is false, so won't allow
	 * to create the user, however the Idp will be the final responsable to
	 * decided if the user could be or not created.
	 */
	DOTCMS_SAML_POLICY_ALLOW_CREATE("policy.allowcreate"),
	
	/**
	 * Used to get the Saml protocol binding, by default use
	 * SamlConstants#SAML2_ARTIFACT_BINDING_URI
	 */
	DOTCMS_SAML_PROTOCOL_BINDING("protocol.binding"),
	
	/**
	 * Optional Key. By default dotcms do not use any filter, but you can
	 * override it just adding the filter you want. For instance, sometimes LDAP
	 * providers use a prefix for an external roles or so, you can remove this
	 * prefix by setting this prop.
	 */
	DOT_SAML_REMOVE_ROLES_PREFIX("remove.roles.prefix"),
	
	/**
	 * By default dotcms use: "authorisations", but you can override it just
	 * adding the roles attribute name you want. "authorisations" will be the
	 * expected field name from the Response comming from the OpenSaml post
	 * call.
	 */
	DOT_SAML_ROLES_ATTRIBUTE("attribute.roles.name"),
	
	/**
	 * In case you need a custom credentials for the Service Provider (DotCMS)
	 * overrides the implementation class on the configuration properties.
	 */
	DOT_SAML_SERVICE_PROVIDER_CUSTOM_CREDENTIAL_PROVIDER_CLASSNAME("service.provider.custom.credential.provider.classname"),
	
	/**
	 * By default we do not include the encryptor in the metadata, if you want
	 * to include it set this to true.
	 */
	DOTCMS_SAML_USE_ENCRYPTED_DESCRIPTOR("use.encrypted.descriptor"),
	
	/**
	 * By default the system will do the verification of the signature
	 * credentials, if for some reason you want to avoid it feel free to set it
	 * to "false".
	 */
	DOT_SAML_VERIFY_SIGNATURE_CREDENTIALS("verify.signature.credentials"),
	
	/**
	 * By default the system will do the verification of the profile signature,
	 * if for some reason you want to avoid it feel free to set it to "false".
	 */
	DOT_SAML_VERIFY_SIGNATURE_PROFILE("verify.signature.profile"),
	//@formatter:on

	/**
	 * By default any query string included on the endpoints Locations from the IDP metadata will be removed in the moment to redirect to the IDP endpoint.
	 * However you can set this on false in order to keep any query string parameter on the IDP metadata
	 */
	DOTCMS_SAML_CLEAR_LOCATION_QUERY_PARAMS("location.cleanqueryparams"),

	/**
	 * By default if the creation of the user fails because a duplicated email, we will try to create a new email based on the name id or a UUID.
	 * If you want the code to set a domain for the new email you can set up it here. For example: dotcms.com
	 */
	DOTCMS_SAML_COMPANY_EMAIL_DOMAIN("company.email.domain"),

	/**
	 * By default after each login, and if the user already exists, we will try to update the name, lastname and email with the values from the assertion
	 * If you want the code to ignore the email you should set this value to false.
	 */
	DOTCMS_SAML_LOGIN_UPDATE_EMAIL("login.email.update"),

	/**
	 * By default the authentication uses the Http-Redirect but you can set to Http-POST if needed
	 *
	 */
	DOTCMS_SAML_AUTHN_PROTOCOL_BINDING("authn.protocol.binding"),

	/**
	 * In case the session wants to be renew, should be true by default.
	 */
	DOT_RENEW_SESSION("renew.session"),

	/**
	 * In case want do a substitution on all roles:
	 * role.key.substitution=/_sepsep_/ /
	 * That substitution expression will replace the string _sepsep_ with white space before matching role name from IdP against role key from dotCMS.
	 */
	DOT_SAML_ROLE_KEY_SUBSTITUTION("role.key.substitution"),

	/**
	 * In case you want to use a different strategy to map the group roles from the IdP to the role keys in dotCMS.
	 */
	DOTCMS_SAML_ROLE_GROUP_MAPPING_STRATEGY("role.group.mapping.strategy"),

	/**
	 * This contains the mapping by content type configuration based on contenttype-varname,contenttype-key,contenttype-value
	 * Where contenttype-varname is the var name of the content type which encapsulates the role mapping group
	 * contenttype-key: is the content type property var name that index the group key
	 * contenttype-value: is the collection of 1 to N role keys that maps this group key
	 */
	DOT_SAML_ROLES_GROUP_MAPPING_BY_CONTENT_TYPE("saml.roles.group.mapping.bycontenttype");

	private final String propertyName;

	SamlName(final String propertyName) {
		this.propertyName = propertyName;
	}

	public String getPropertyName() {
		return propertyName;
	}


	/**
	 * Find the enum constant based on propertyName ;
	 * 
	 * @param propertyName
	 * @return found enum or null if not found.
	 */

	public static SamlName findProperty(final String propertyName) {

		for (final SamlName samlName : values()) {
			
			if (samlName.getPropertyName().equals(propertyName)) {

				return samlName;
			}
		}

		return null;
	}

	@Override
	public String toString() {
		return "SamlName{" +
				"propertyName='" + propertyName + '\'' +
				'}';
	}
}
