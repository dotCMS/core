package com.dotcms.contenttype.exception;

import com.dotmarketing.exception.DotDataException;

public class DotDataValidationException extends DotDataException {

	final String i18nKey;
	private static final long serialVersionUID = 1L;
	
	public DotDataValidationException(Throwable e) {
		super(e.getMessage(), e);
		this.i18nKey=e.getMessage();
	}
	
	public DotDataValidationException(String englishMessage, String i18nKey) {
		super(englishMessage);
		this.i18nKey=i18nKey;
	}

	public DotDataValidationException(String englishMessage, String i18nKey, Throwable e) {
		super(englishMessage, e);
		this.i18nKey=i18nKey;
	}
	
	public String i18nKey(){
		return i18nKey;
	}

}
