package com.dotmarketing.exception;

/**
 * @todo remove the setMessage method, replace uses with 'throw new WhateverException(message, oldException)'
 */
public class DotDataException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7641394178731435069L;

	private String message;
	public DotDataException(Throwable t) {
		super(t.getMessage(),t);
		this.message = t.getMessage();
	}
	
	public DotDataException(String message) {
		this.message = message;
	}
	public DotDataException(String message, Throwable e) {
        super(message, e);
		this.message = message;
	}
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
}
