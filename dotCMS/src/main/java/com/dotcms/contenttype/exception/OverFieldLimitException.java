package com.dotcms.contenttype.exception;

import com.dotmarketing.exception.DotDataException;

public class OverFieldLimitException extends DotDataException {


	private static final long serialVersionUID = 1L;

	public OverFieldLimitException(String message) {
		super(message);

	}

	public OverFieldLimitException(String message, Throwable e) {
		super(message, e);

	}

}
