package com.dotcms.contenttype.exception;

import com.dotmarketing.exception.DotDataException;

public class NotFoundInDbException extends DotDataException {


	private static final long serialVersionUID = 1L;

	public NotFoundInDbException(String message) {
		super(message);

	}

	public NotFoundInDbException(String message, Throwable e) {
		super(message, e);

	}

}
