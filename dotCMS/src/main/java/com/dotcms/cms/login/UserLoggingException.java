package com.dotcms.cms.login;

/**
 * Created by freddyrodriguez on 8/10/16.
 */
public class UserLoggingException extends RuntimeException{

    public UserLoggingException(Throwable rootCause){
        super(rootCause);
    }
}
