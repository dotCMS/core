package com.dotcms.saml.service.external;

import javax.servlet.http.HttpServletResponse;

/**
 * When an email is not and configuration says it is not allowed.
 * @author jsanca
 */
public class NotNullEmailAllowedException extends AttributesNotFoundException {
	private static final long serialVersionUID = -3622432364873488814L;

	public NotNullEmailAllowedException() {
		
	}

	public NotNullEmailAllowedException(final String message) {
		super( message );
	}

	public NotNullEmailAllowedException(final String message, final Throwable cause) {
		super( message, cause );
	}

	public int getStatus() {
		return HttpServletResponse.SC_UNAUTHORIZED;
	}
}
