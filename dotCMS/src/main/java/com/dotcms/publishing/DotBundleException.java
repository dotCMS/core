package com.dotcms.publishing;

public class DotBundleException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	private String message;
	
	public DotBundleException(String message) {
		this.message = message;
	}
	
	public DotBundleException(String message, Exception e) {
		this.message = message;
		super.initCause(e);
	}
	
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	
	
	@Override
	public String getLocalizedMessage() {
		return message;
	}
	
	
	
}
