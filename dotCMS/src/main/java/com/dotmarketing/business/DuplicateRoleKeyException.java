package com.dotmarketing.business;

/**
 * 
 * Exception thrown when another role with the same key already exists in the system
 * @author David Torres
 *
 */
public class DuplicateRoleKeyException extends DotStateException {

	private static final long serialVersionUID = 1L;

	public DuplicateRoleKeyException(String x) {
		super(x);
	}

}
