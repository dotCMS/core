/**
 * 
 */
package com.dotcms.util.exceptions;

/**
 * @author Brent Griffin
 *
 */
public class DuplicateFileException extends Exception {
	private static final long serialVersionUID = 6533100284620774467L;
		
	public DuplicateFileException() {
		super();
	}
	
	public DuplicateFileException(String message) {
		super(message);
	}
	
	public DuplicateFileException(Throwable t) {
		super(t);
	}
	
	public DuplicateFileException(String message, Throwable t) {
		super(message, t);
	}
}
