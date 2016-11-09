/**
 * 
 */
package com.dotmarketing.business;

/**
 * @author Carlos Rivas
 *
 */
public class PermissionNotFoundException extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5385761307831095807L;
	private String message;
	
	public PermissionNotFoundException(String message) {
		this.message = message;
	}
	public PermissionNotFoundException(String message, Exception e) {
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
