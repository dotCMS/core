/*
 * ServletResponseCharacterEncoding.java
 *
 * Created on 24 October 2007
 */
package com.dotmarketing.util;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Ensures that the <tt>ServletResponse</tt> is properly encoded 
 * setting the character set whenever needed.
 * 
 * Code based on JavaWorld Article by Mike Gavaghan 
 * {@link http://www.javaworld.com/javaworld/jw-05-2004/jw-0524-i18n.html?page=4 }
 * 
 * @author Mike Gavaghan
 * @author Dimitris Zavaliadis
 * 
 * @version 1.0
 */

public class ServletResponseCharacterEncoding extends HttpServletResponseWrapper {

	private boolean encodingSpecified = false;

	public ServletResponseCharacterEncoding(HttpServletResponse response) {
		super(response);
	}

	@Override
	public void setContentType(String type) {
		String explicitType = type;
		// If a specific encoding has not already been set by the app, 
		if (!encodingSpecified && type != null) {
			// see if this is a call to explicitly set the character encoding.
			String lowerType = type.toLowerCase();
			if (lowerType.indexOf("charset") < 0) {
				// If no character encoding is specified, we still need to
				// ensure the app is specifying text content.
				if (lowerType.startsWith("text/")) {
					// App is sending a text response, but no encoding
					// is specified, so we'll set it according to the configured charset.
					explicitType = type + ";charset=" + UtilMethods.getCharsetConfiguration();
				}
			}
			else {
				// App picked a specific encoding, so let's make
				// sure we don't override it.
				encodingSpecified = true;
			}
		}

		// Delegate to supertype to record encoding
		super.setContentType(explicitType);
	}
	
	/**
	 * Send a temporary redirect to the specified redirect location URL.
	 *
	 * @param location Location URL to redirect to
	 *
	 * @exception IllegalStateException if this response has
	 *  already been committed
	 * @exception IOException if an input/output error occurs
	 */
	@Override
	public void sendRedirect(String location) 
	throws IOException {
		// Generate a temporary redirect to the specified location
		try {
			setStatus(301); 
			setHeader( "Location", location ); 
			setHeader( "Connection", "close" ); 
		} catch (IllegalArgumentException e) {
			setStatus(SC_NOT_FOUND);
		}
	}

}
