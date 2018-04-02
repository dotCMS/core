package com.dotcms.api.system.event;

import com.dotcms.exception.BaseRuntimeInternationalizationException;

/**
 * When an event can not be push on the queue, throws this exception
 */
public class CanNotPushSystemEventException extends BaseRuntimeInternationalizationException {
    public CanNotPushSystemEventException(final String message) {
        super(message);
    }

    public CanNotPushSystemEventException(final Throwable e) {
        super(e);
    }
}
