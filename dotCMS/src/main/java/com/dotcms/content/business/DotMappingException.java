package com.dotcms.content.business;

import com.dotmarketing.exception.DotRuntimeException;

public class DotMappingException extends DotRuntimeException {

    private static final long serialVersionUID = -5613964763936171392L;

    public DotMappingException(String message) {
        super(message);
    }

    public DotMappingException(String message, Throwable cause) {
        super(message, cause);
    }

}
