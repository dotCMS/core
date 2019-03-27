package com.dotmarketing.business;

/**
 * Used for throwing identifier problems
 * @author Jason Tesser
 *
 */
public class DotIdentifierStateException extends DotStateException {

	private static final long serialVersionUID = 1L;

	/**
	 * Used for throwing identifier problems
	 * @param x
	 */
	public DotIdentifierStateException(String x) {
		super(x);
	}
	
	/**
	 * Used for throwing identifier problems
	 * @param x
	 * @param e
	 */
	public DotIdentifierStateException(String x, Exception e) {
		super(x, e);
	}
	
}
