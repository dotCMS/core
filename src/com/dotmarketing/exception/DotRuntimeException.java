/*
 * Created on Aug 6, 2004
 *
 */
package com.dotmarketing.exception;

import java.io.PrintStream;
import java.io.PrintWriter;

public class DotRuntimeException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	private String message;
	
	public DotRuntimeException(String x){
		
		this.message = x;
		
	}

	public DotRuntimeException(String x, Exception e){
		
		this.message = x;
		super.initCause(e);
		
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Throwable#fillInStackTrace()
	 */
	public synchronized Throwable fillInStackTrace() {
		
		return super.fillInStackTrace();
	}
	/* (non-Javadoc)
	 * @see java.lang.Throwable#getCause()
	 */
	public Throwable getCause() {
		
		return super.getCause();
	}
	/* (non-Javadoc)
	 * @see java.lang.Throwable#getLocalizedMessage()
	 */
	public String getLocalizedMessage() {
		
		return super.getLocalizedMessage();
	}
	/* (non-Javadoc)
	 * @see java.lang.Throwable#getMessage()
	 */
	public String getMessage() {
		
		return message;
	}
	/* (non-Javadoc)
	 * @see java.lang.Throwable#getStackTrace()
	 */
	public StackTraceElement[] getStackTrace() {
		
		return super.getStackTrace();
	}
	/* (non-Javadoc)
	 * @see java.lang.Throwable#initCause(java.lang.Throwable)
	 */
	public synchronized Throwable initCause(Throwable arg0) {
		
		return super.initCause(arg0);
	}
	/* (non-Javadoc)
	 * @see java.lang.Throwable#printStackTrace()
	 */
	public void printStackTrace() {
		
		super.printStackTrace();
	}
	/* (non-Javadoc)
	 * @see java.lang.Throwable#printStackTrace(java.io.PrintStream)
	 */
	public void printStackTrace(PrintStream arg0) {
		
		super.printStackTrace(arg0);
	}
	/* (non-Javadoc)
	 * @see java.lang.Throwable#printStackTrace(java.io.PrintWriter)
	 */
	public void printStackTrace(PrintWriter arg0) {
		
		super.printStackTrace(arg0);
	}
	/* (non-Javadoc)
	 * @see java.lang.Throwable#setStackTrace(java.lang.StackTraceElement[])
	 */
	public void setStackTrace(StackTraceElement[] arg0) {
		
		super.setStackTrace(arg0);
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		
		return super.toString();
	}
}
