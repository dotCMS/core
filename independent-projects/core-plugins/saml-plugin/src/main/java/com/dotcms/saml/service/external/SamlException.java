package com.dotcms.saml.service.external;

/**
 * Exception to report things related to the dot saml exception
 * 
 * @author jsanca
 */
public class SamlException extends RuntimeException {
	private static final long serialVersionUID = -3569526825729783600L;

	public SamlException(){
		super();
	}

	public SamlException(final String message) {
		super( message );
	}

	public SamlException(final String message, final Throwable cause) {
		super( message, cause );
	}
}
