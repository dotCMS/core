package com.dotcms.enterprise.publishing.staticpublishing;

public class EndPointPublisherConnectionException extends RuntimeException{

    public EndPointPublisherConnectionException(final Throwable cause){
        super(cause.getMessage(), cause);
    }

}
