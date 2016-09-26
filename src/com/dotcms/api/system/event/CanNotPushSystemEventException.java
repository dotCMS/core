package com.dotcms.api.system.event;

import com.dotcms.exception.BaseRuntimeInternationalizationException;
import com.dotmarketing.exception.DotDataException;

/**
 * Created by freddyrodriguez on 23/9/16.
 */
public class CanNotPushSystemEventException extends BaseRuntimeInternationalizationException {
    public CanNotPushSystemEventException(Throwable e) {
        super(e);
    }
}
