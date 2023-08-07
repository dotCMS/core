package com.dotcms.experiments.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;

/**
 * Exception throw when something is wrong in any {@link RegexUrlPatterStrategy}'s operation.
 */
public class RegexUrlPatterStrategyException extends DotRuntimeException {

    public RegexUrlPatterStrategyException(final Exception e) {
        super(e);
    }
}
