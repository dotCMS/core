package com.dotcms.rest.exception.mapper.badrequest;
import com.dotcms.rest.exception.mapper.DotBadRequestExceptionMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

public class IllegalArgumentExceptionMapper
        extends DotBadRequestExceptionMapper<IllegalArgumentException> {}
