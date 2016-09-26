package com.dotmarketing.exception;

public class AlreadyExistException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String message;
	
	public AlreadyExistException(String message) {
		this.message = message;
	}
	public AlreadyExistException(String message, Exception e) {
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
