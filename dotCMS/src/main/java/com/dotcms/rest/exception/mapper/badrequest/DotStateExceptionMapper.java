package com.dotcms.rest.exception.mapper.badrequest;
import com.dotcms.rest.exception.mapper.DotBadRequestExceptionMapper;
import com.dotmarketing.business.DotStateException;
import javax.ws.rs.ext.Provider;

@Provider
public class DotStateExceptionMapper
        extends DotBadRequestExceptionMapper<DotStateException> {

}