package com.dotmarketing.business;

import com.dotmarketing.exception.DotRuntimeException;

public class DotInvalidPasswordException extends DotRuntimeException {

    public DotInvalidPasswordException(String x) {
        super(x);
    }

}
