package com.dotcms.saml.service.external;

import org.opensaml.security.credential.Credential;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the metadata bean stored in the idp metadata xml file
 * The meta is usually an XML sent from IDP (aka Azure, Okta, etc)
 * The metadata.xml contains the public key, the endpoint to login/logout
 * and many other information to interact with the IDP
 * @author jsanca
 */
public class MetaData implements Serializable {
	private static final long serialVersionUID = -2988699344160818527L;

	// The entity id on the xml
	private final String entityId;

	// The error url
	private final String errorURL;

	// List of single sign on location indexed by binding name
	private final Map<String, String> singleSignOnBindingLocationMap;

	// List of single logout on location indexed by binding name
	private final Map<String, String> singleLogoutBindingLocationMap;

	// Credential signing list
	private final List<Credential> credentialSigningList;

	public MetaData(final String entityId, final String errorURL,
					final Map<String, String> singleSignOnBindingLocationMap,
					final Map<String, String> singleLogoutBindingLocationMap,
					final List<Credential> credentialSigningList) {

		this.entityId = entityId;
		this.errorURL = errorURL;
		this.singleSignOnBindingLocationMap = singleSignOnBindingLocationMap;
		this.credentialSigningList = credentialSigningList;
		this.singleLogoutBindingLocationMap = singleLogoutBindingLocationMap;
	}

	public String getEntityId() {
		return entityId;
	}

	public String getErrorURL() {
		return errorURL;
	}

	public Map<String, String> getSingleSignOnBindingLocationMap() {
		return singleSignOnBindingLocationMap;
	}

	public List<Credential> getCredentialSigningList() {
		return credentialSigningList;
	}

	public Map<String, String> getSingleLogoutBindingLocationMap() {
		return singleLogoutBindingLocationMap;
	}
}
