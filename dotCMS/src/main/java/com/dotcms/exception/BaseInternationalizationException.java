package com.dotcms.exception;

/**
 * This class extends from Exception and implements the internationalizationExceptionSupport interface, 
 * allowing to pass a messageKey and messageArguments to translate those exceptions messages into the 
 * current request locale language
 * @author oswaldogallango
 *
 */
public class BaseInternationalizationException extends Exception implements InternationalizationExceptionSupport {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final String messageKey;
	private final Object[] messageArguments;
	
	public BaseInternationalizationException() {
		super();
		this.messageKey=null;
		this.messageArguments=null;
	}

	public BaseInternationalizationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.messageKey=null;
		this.messageArguments=null;
	}

	public BaseInternationalizationException(String message, Throwable cause) {
		super(message, cause);
		this.messageKey=null;
		this.messageArguments=null;
	}

	public BaseInternationalizationException(String message) {
		super(message);
		this.messageKey=null;
		this.messageArguments=null;
	}

	public BaseInternationalizationException(Throwable cause) {
		super(cause);
		this.messageKey=null;
		this.messageArguments=null;
	}

	public BaseInternationalizationException(String message, Throwable cause, String messageKey, Object... messageArguments) {
		super(message, cause);
		this.messageKey=messageKey;
		this.messageArguments=messageArguments;
	}

	public BaseInternationalizationException(String message, String messageKey, Object... messageArguments) {
		super(message);
		this.messageKey=messageKey;
		this.messageArguments=messageArguments;
	}

	@Override
	public String getMessageKey() {
		return messageKey;
	}

	@Override
	public Object[] getMessageArguments() {
		return messageArguments;
	}

	
}
