package com.dotcms.experiments.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;

public class RegexUrlPatterStrategyException extends DotRuntimeException {

    public RegexUrlPatterStrategyException(final Exception e) {
        super(e);
    }
}
