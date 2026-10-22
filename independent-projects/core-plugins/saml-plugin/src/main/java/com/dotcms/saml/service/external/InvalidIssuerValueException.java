package com.dotcms.saml.service.external;

/**
 * Exception to report an issue with the issuer value.
 * 
 * @author jsanca
 */

public class InvalidIssuerValueException extends RuntimeException {
	private static final long serialVersionUID = 2963820217308468676L;

	public InvalidIssuerValueException(final String message) {
		super(message);
	}
}
