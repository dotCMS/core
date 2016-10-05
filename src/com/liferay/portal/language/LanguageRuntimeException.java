package com.liferay.portal.language;

/**
 * Thrown when a unexpected error occur while it is trying to get the current locale
 */
public class LanguageRuntimeException extends RuntimeException {

    public LanguageRuntimeException(Throwable e){
        super(e);
    }
}
