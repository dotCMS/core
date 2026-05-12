package com.dotcms.queue;

import com.dotmarketing.exception.DotRuntimeException;

/**
 * Unchecked exception thrown when a queue operation fails. Wraps provider-specific
 * errors so callers are not coupled to a particular queue implementation.
 */
public class DotQueueException extends DotRuntimeException {

    private static final long serialVersionUID = 1L;

    public DotQueueException(final String message) {
        super(message);
    }

    public DotQueueException(final Throwable cause) {
        super(cause);
    }

    public DotQueueException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
