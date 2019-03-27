/*
 * Created on Aug 6, 2004
 *
 */
package com.dotmarketing.exception;


public class DotRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public DotRuntimeException(String message){
		super(message);
	}
	public DotRuntimeException(Throwable cause){
		this(cause.getMessage(),cause);
	}
	public DotRuntimeException(String message, Throwable cause){
		super(message, cause);
	}
}
