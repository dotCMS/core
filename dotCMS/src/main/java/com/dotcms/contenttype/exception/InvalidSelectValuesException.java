package com.dotcms.contenttype.exception;

import com.dotmarketing.exception.DotDataException;

public class InvalidSelectValuesException extends DotDataValidationException {


	private static final long serialVersionUID = 1L;

	public InvalidSelectValuesException(String message, String i18nKey, Throwable e) {
		super(message, i18nKey, e);

	}

	public InvalidSelectValuesException(String message, String i18nKey) {
		super(message, i18nKey);

	}

	public InvalidSelectValuesException(Throwable e) {
		super(e);

	}



}
