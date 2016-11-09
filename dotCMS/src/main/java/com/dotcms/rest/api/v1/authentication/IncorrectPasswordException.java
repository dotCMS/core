package com.dotcms.rest.api.v1.authentication;

import com.dotcms.exception.BaseInternationalizationException;

/**
 * Thrown when a password match fail
 */
public class IncorrectPasswordException extends BaseInternationalizationException {

    public IncorrectPasswordException(){
        super("current password is incorrect", "current.usermanager.password.incorrect");
    }
}
