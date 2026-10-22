package com.dotcms.saml.service.external;

/**
 * To report unauthorized issues.
 * 
 * @author jsanca
 */
public class SamlUnauthorizedException extends SamlException {
	private static final long serialVersionUID = 2827175662161844965L;
	private final int status;
	private final String unauthorizedPage;

	public SamlUnauthorizedException(final String message)
	{
		this( message, 401, "/html/error/custom-error-page.jsp" );
	}

	public SamlUnauthorizedException(final String message, final Throwable cause) {
		this( message, cause, 401, "/html/error/custom-error-page.jsp" );
	}

	public SamlUnauthorizedException(final String message, final int status, final String unauthorizedPage) {
		super( message );
		this.status = status;
		this.unauthorizedPage = unauthorizedPage;
	}

	public SamlUnauthorizedException(final String message, final Throwable cause, final int status, final String unauthorizedPage) {
		super( message, cause );
		this.status = status;
		this.unauthorizedPage = unauthorizedPage;
	}

	public int getStatus() {
		return status;
	}

	public String getUnauthorizedPage() {
		return unauthorizedPage;
	}
}
