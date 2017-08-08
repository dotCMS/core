package com.dotcms.aspects;

import com.dotmarketing.exception.DotRuntimeException;

/**
 * Exception for reporting aspects errors
 * @author jsanca
 */
public class DotAspectException extends DotRuntimeException {

    public DotAspectException(String x) {
        super(x);
    }

    public DotAspectException(Throwable x) {
        super(x);
    }

    public DotAspectException(String x, Throwable e) {
        super(x, e);
    }
}
