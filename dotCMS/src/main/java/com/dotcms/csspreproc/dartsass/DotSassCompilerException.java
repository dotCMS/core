package com.dotcms.csspreproc.dartsass;

import com.dotmarketing.exception.DotDataException;

/**
 *
 * @author Jose Castro
 * @since Jul 27th, 2022
 */
public class DotSassCompilerException extends DotDataException {

    public DotSassCompilerException(final String message) {
        super(message);
    }

    public DotSassCompilerException(String message, Throwable e) {
        super(message, e);
    }

}
