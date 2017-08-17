package com.dotcms.cms.login;

import com.dotmarketing.exception.DotSecurityException;


public class LoginSecurityException extends DotSecurityException{

    public LoginSecurityException(String message) {
        super(message);

    }

    public LoginSecurityException(String message, Exception e) {
        super(message, e);

    }


}
