/*
 * Created on Aug 6, 2004
 *
 */
package com.dotmarketing.exception;


public class DotRuntimeException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	
	public DotRuntimeException(String x){
		super(x);

		
	}
	public DotRuntimeException(Throwable x){
		this(x.getMessage(),x);
	}
	public DotRuntimeException(String x, Throwable e){
		super(x, e);
	}

}
