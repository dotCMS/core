package com.dotcms.enterprise.license;

import java.io.IOException;

public class DotInvalidLicenseException extends Exception {
	String message;
	@Override
	public String getMessage() {
		// TODO Auto-generated method stub
		return message;
	}

	public DotInvalidLicenseException(String string, IOException e) {
		this.message = string;
		initCause(e);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;




	
}
