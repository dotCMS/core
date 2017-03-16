package com.dotmarketing.exception;

public class DotSecurityException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	public DotSecurityException(String message) {
		super(message);
	}
	public DotSecurityException(String message, Exception e) {
		this(message);
		super.initCause(e);
	}
	
}
