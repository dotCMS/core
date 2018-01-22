package com.dotcms.aspects;

import com.dotmarketing.exception.DotRuntimeException;

/**
 * Exception for reporting aspects errors
 * @author jsanca
 */
public class DotAspectException extends DotRuntimeException {

    public DotAspectException(final String x) {
        super(x);
    }

    public DotAspectException(final Throwable x) {
        super(x);
    }

    public DotAspectException(final String x, final Throwable e) {
        super(x, e);
    }
}
