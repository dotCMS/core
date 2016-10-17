package com.dotcms.content.elasticsearch.business;

public class DotIndexException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String message;

	public DotIndexException(String message) {
		this.message = message;
	}

	public String getMessage() {

		return message;
	}

	@Override
	public String toString() {

		return message + super.toString();
	}

	public String getLocalizedMessage() {

		return message;
	}

}
