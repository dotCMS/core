/**
 * 
 */
package com.dotmarketing.business;

/**
 * @author Carlos Rivas
 *
 */
public class DataAccessException extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4341568856831804802L;
	/**
	 * 
	 */
	private String message;
	
	public DataAccessException(String message) {
		this.message = message;
	}
	public DataAccessException(String message, Exception e) {
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
