package com.dotcms.contenttype.exception;

import com.dotmarketing.exception.DotDataException;

public abstract class DataValidationException extends DotDataException {

	final String i18nKey;
	private static final long serialVersionUID = 1L;
	
	public DataValidationException(Throwable e) {
		super(e.getMessage(), e);
		this.i18nKey=e.getMessage();
	}
	
	public DataValidationException(String message, String i18nKey) {
		super(message);
		this.i18nKey=i18nKey;
	}

	public DataValidationException(String message, String i18nKey, Throwable e) {
		super(message, e);
		this.i18nKey=i18nKey;
	}
	
	public String i18nKey(){
		return i18nKey;
	}

}
