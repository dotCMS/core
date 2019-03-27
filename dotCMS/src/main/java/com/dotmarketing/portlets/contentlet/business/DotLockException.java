package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.exception.DotSecurityException;

public class DotLockException extends DotSecurityException {

	public DotLockException(String message) {
		super(message);
	}
	public DotLockException(String message, Exception e) {
		super(message, e);
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
