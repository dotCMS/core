package com.dotcms.cli.exception;

public interface ExceptionHandler {

    Exception unwrap(Exception ex);

    Exception handle(Exception ex);

}
