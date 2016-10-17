package com.dotmarketing.exception;

public class DotHibernateException extends DotDataException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DotHibernateException(String message) {
		super(message);
	}
	
	public DotHibernateException(String message, Exception e) {
		super(message, e);
	}

}
