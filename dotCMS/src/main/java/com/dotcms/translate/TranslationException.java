package com.dotcms.translate;

public class TranslationException extends Exception {

	private static final long serialVersionUID = 1L;

	public TranslationException() {
		super();
	}

	public TranslationException(String message) {
		super(message);
	}

	public TranslationException(String message, Throwable cause) {
		super(message, cause);
	}

	public TranslationException(Throwable cause) {
		super(cause);
	}
}
