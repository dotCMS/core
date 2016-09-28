package com.dotmarketing.business;

/**
 * Created by freddyrodriguez on 31/8/16.
 */
public class LoginAsRuntimeException extends RuntimeException {
    public LoginAsRuntimeException(Exception e) {
        super(e);
    }
}
