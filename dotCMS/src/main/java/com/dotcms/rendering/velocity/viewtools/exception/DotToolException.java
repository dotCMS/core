package com.dotcms.rendering.velocity.viewtools.exception;

import com.dotmarketing.exception.DotRuntimeException;

public class DotToolException extends DotRuntimeException {
    private static final long serialVersionUID = 1L;

    public DotToolException(Throwable x){
        super(x.getMessage(),x);
    }

    public DotToolException(String x) {
        super(x);
    }

    public DotToolException(String x, Exception cause) {
        super(x, cause);
    }
}

