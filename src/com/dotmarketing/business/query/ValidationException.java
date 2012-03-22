package com.dotmarketing.business.query;

import com.dotmarketing.business.DotStateException;

/**
 * Used for throwing QueryBuilder problems
 * @author Jason Tesser
 * @since 1.9
 */
public class ValidationException extends DotStateException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Used for throwing contentlet problems
	 * @param x
	 */
	public ValidationException(String x) {
		super(x);
	}
	
	/**
	 * Used for throwing contentlet problems
	 * @param x
	 * @param e
	 */
	public ValidationException(String x, Exception e) {
		super(x, e);
	}
}
