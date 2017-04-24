package com.dotmarketing.exception;

/**
 * Indicates a data issue where duplicate records are not allowed.
 * 
 * @author Jose Castro
 * @version 4.1.0
 * @since Apr 17, 2017
 *
 */
public class DotDuplicateDataException extends DotRuntimeException {

	private static final long serialVersionUID = -2142585259996263444L;

	/**
	 * Constructs a new runtime exception with the specified detail message. The
	 * cause is not initialized, and may subsequently be initialized by a call
	 * to {@link #initCause}.
	 * 
	 * @param msg
	 *            - The detail message is saved for later retrieval by the
	 *            getMessage() method.
	 */
	public DotDuplicateDataException(String msg) {
		super(msg);
	}

	/**
	 * Constructs a new runtime exception with the specified cause.
	 * 
	 * @param throwable
	 *            - The cause (which is saved for later retrieval by the
	 *            getCause() method). (A null value is permitted, and indicates
	 *            that the cause is nonexistent or unknown.)
	 */
	public DotDuplicateDataException(Throwable throwable) {
		this(throwable.getMessage(), throwable);
	}

	/**
	 * Constructs a new runtime exception with the specified detail message and
	 * cause. Note that the detail message associated with cause is not
	 * automatically incorporated in this runtime exception's detail message.
	 * 
	 * @param msg
	 *            - The detail message (which is saved for later retrieval by
	 *            the getMessage() method).
	 * @param throwable
	 *            - The cause (which is saved for later retrieval by the
	 *            getCause() method). (A null value is permitted, and indicates
	 *            that the cause is nonexistent or unknown.)
	 */
	public DotDuplicateDataException(String msg, Throwable throwable) {
		super(msg, throwable);
	}

}
