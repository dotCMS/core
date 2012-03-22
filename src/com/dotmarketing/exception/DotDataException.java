package com.dotmarketing.exception;

public class DotDataException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7641394178731435069L;

	private String message;
	
	public DotDataException(String message) {
		this.message = message;
	}
	public DotDataException(String message, Exception e) {
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
