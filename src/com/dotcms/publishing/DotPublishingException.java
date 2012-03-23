package com.dotcms.publishing;

public class DotPublishingException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	private String message;
	
	public DotPublishingException(String message) {
		this.message = message;
	}
	
	public DotPublishingException(String message, Exception e) {
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
