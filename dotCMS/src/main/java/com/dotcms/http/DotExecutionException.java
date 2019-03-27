package com.dotcms.http;

import com.dotmarketing.exception.DotRuntimeException;

public class DotExecutionException extends DotRuntimeException {


    private static final long serialVersionUID = 1L;

    public DotExecutionException(Exception ex) {
        super(ex);
    }
    
    public DotExecutionException(String msg) {
        super(msg);
    }
    
    public DotExecutionException(String msg, Exception e) {
        super(msg,e);
    }
}
