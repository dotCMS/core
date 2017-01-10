package com.dotmarketing.exception;

public class DotSecurityException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String message;
	
	public DotSecurityException(String message) {
		this.message = message;
	}
	public DotSecurityException(String message, Exception e) {
		this.message = message;
		super.initCause(e);
	}
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	
}
