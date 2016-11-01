package com.dotmarketing.business;

/**
 * Used for throwing an error when no user can be found
 * @author Jason Tesser
 *
 */
public class NoSuchUserException extends DotStateException {

	private static final long serialVersionUID = 1L;

	/**
	 * Used for throwing identifier problems
	 * @param x
	 */
	public NoSuchUserException(String x) {
		super(x);
	}
	
	/**
	 * Used for throwing identifier problems
	 * @param x
	 * @param e
	 */
	public NoSuchUserException(String x, Exception e) {
		super(x, e);
	}
	
}
