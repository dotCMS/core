package com.dotmarketing.business;

public class PublishStateException extends DotStateException {
    private static final long serialVersionUID = -1005946078823347507L;
    public PublishStateException(String x) {
        super(x);
    }
    public PublishStateException(String x, Exception cause) {
        super(x,cause);
    }
}
