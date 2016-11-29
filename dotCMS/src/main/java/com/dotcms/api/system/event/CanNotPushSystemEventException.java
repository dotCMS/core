package com.dotcms.api.system.event;

import com.dotcms.exception.BaseRuntimeInternationalizationException;
import com.dotmarketing.exception.DotDataException;

/**
 * When an event can not be push on the queue, throws this exception
 */
public class CanNotPushSystemEventException extends BaseRuntimeInternationalizationException {
    public CanNotPushSystemEventException(Throwable e) {
        super(e);
    }
}
