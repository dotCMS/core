package com.dotcms.rest.exception.mapper;

import com.dotcms.rest.exception.BadRequestException;
import javax.ws.rs.ext.Provider;

@Provider
public class DefaultDotBadRequestExceptionMapper extends DotBadRequestExceptionMapper<BadRequestException> {
}
