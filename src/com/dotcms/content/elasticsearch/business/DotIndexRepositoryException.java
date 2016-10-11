package com.dotcms.content.elasticsearch.business;

import com.dotcms.exception.BaseRuntimeInternationalizationException;

public class DotIndexRepositoryException extends BaseRuntimeInternationalizationException {
	public DotIndexRepositoryException(Throwable e) {
        super(e);
    }

	public DotIndexRepositoryException(String message, String messageKey, Object... messageArguments) {
		super(message, messageKey, messageArguments);
	}
}
