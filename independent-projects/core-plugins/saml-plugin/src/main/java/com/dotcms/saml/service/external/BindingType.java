package com.dotcms.saml.service.external;

/**
 * Encapsulates the idp meta datas binding type names
 * The Binding type tells into the XML which method to use on the SAML process
 * @author jsanca
 */
public enum BindingType {

	AUTHN_REQUEST("urn:mace:shibboleth:1.0:profiles:AuthnRequest"),
	POST         ("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"),
	REDIRECT     ("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect");

	private final String binding;

	private BindingType(final String value)
	{
		this.binding = value;
	}

	public String getBinding()
	{
		return binding;
	}
}
