package com.dotcms.rest.exception.mapper;

import com.dotcms.rest.exception.ConflictException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Provider
public class DotConflictExceptionMapper extends DotExceptionMapper<ConflictException>{

    @Override
    protected String getErrorKey() {
        return "conflict";
    }

    @Override
    protected Status getErrorStatus() {
        return Status.CONFLICT;
    }
}
