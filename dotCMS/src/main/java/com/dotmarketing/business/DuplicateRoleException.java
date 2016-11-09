package com.dotmarketing.business;

/**
 * 
 * Exception thrown when another role with the same path/FQN already exists
 * @author David Torres 
 *
 */
public class DuplicateRoleException extends DotStateException {

	private static final long serialVersionUID = 1L;

	public DuplicateRoleException(String x) {
		super(x);
	}

}
