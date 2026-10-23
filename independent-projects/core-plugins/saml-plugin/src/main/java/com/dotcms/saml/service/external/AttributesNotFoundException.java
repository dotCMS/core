package com.dotcms.saml.service.external;

/**
 * Runtime exception used to handle errors when attributes might not be
 * extracted from the Assertion object Created by nollymar on 3/15/17.
 */
public class AttributesNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 4345557895408407837L;

	public AttributesNotFoundException() {
		
	}

	public AttributesNotFoundException(final String message) {
		super(message);
	}

	public AttributesNotFoundException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
