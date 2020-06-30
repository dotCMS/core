package com.dotcms.saml;

/**
 * Exception to report things related to the dot saml exception
 * 
 * @author jsanca
 */
public class SamlException extends RuntimeException
{
	private static final long serialVersionUID = -3569526825729783600L;

	public SamlException()
	{
		
	}

	public SamlException(String message )
	{
		super( message );
	}

	public SamlException(String message, Throwable cause )
	{
		super( message, cause );
	}
}
