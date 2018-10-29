package com.dotcms.rendering.velocity.viewtools.exception;

import com.dotmarketing.exception.DotRuntimeException;

public class DotToolException extends DotRuntimeException {
    private static final long serialVersionUID = 1L;

    public DotToolException(final Throwable cause){
        super(cause.getMessage(),cause);
    }

    public DotToolException(final String message) {
        super(message);
    }

    public DotToolException(final String message, final Exception cause) {
        super(message, cause);
    }
}

