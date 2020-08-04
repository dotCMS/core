package com.dotcms.saml;

/**
 * Exception to report things related to the dot saml exception
 * 
 * @author jsanca
 */
public class DotSamlException extends RuntimeException {
	
	private static final long serialVersionUID = -3569526825729783600L;

	public DotSamlException() {
		super();
	}

	public DotSamlException(String message )
	{
		super( message );
	}

	public DotSamlException(String message, Throwable cause )
	{
		super( message, cause );
	}
}
