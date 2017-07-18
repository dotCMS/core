package com.dotmarketing.exception;

public class DotSecurityException extends RuntimeException {

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
