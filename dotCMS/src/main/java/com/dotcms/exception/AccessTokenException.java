package com.dotcms.exception;

import com.dotcms.analytics.model.TokenStatus;

public class AccessTokenException extends Exception {

    private final TokenStatus tokenStatus;

    public AccessTokenException(String message, TokenStatus tokenStatus) {
        super(message);
        this.tokenStatus = tokenStatus;
    }

    public AccessTokenException(String message, Throwable cause, TokenStatus tokenStatus) {
        super(message, cause);
        this.tokenStatus = tokenStatus;
    }

}
