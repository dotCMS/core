package com.dotmarketing.business;

/**
 * Used for throwing an error when no user can be found
 * @author Jason Tesser
 *
 */
public class DuplicateUserException extends DotStateException {

	private static final long serialVersionUID = 1L;

	/**
	 * Used for throwing identifier problems
	 * @param x
	 */
	public DuplicateUserException(String x) {
		super(x);
	}
	
	/**
	 * Used for throwing identifier problems
	 * @param x
	 * @param e
	 */
	public DuplicateUserException(String x, Exception e) {
		super(x, e);
	}
	
}
