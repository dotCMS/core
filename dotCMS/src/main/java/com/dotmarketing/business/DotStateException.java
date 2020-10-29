package com.dotmarketing.business;

import com.dotmarketing.exception.DotRuntimeException;

/**
 * 
 * @author David Torres
 * @author Jason Tesser
 * @since 1.6
 */
public class DotStateException extends DotRuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DotStateException(String x) {
		super(x);
	}
	
	public DotStateException(String x, Throwable e) {
		super(x, e);
	}
	
	public DotStateException(Throwable ex) {
		super(ex.getMessage() ,ex);
	}
	
}
