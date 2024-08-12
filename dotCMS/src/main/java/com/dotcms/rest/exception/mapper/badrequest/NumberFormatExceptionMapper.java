package com.dotcms.rest.exception.mapper.badrequest;
import com.amazonaws.services.kms.model.AlreadyExistsException;
import com.dotcms.rest.exception.mapper.DotBadRequestExceptionMapper;

public class NumberFormatExceptionMapper
        extends DotBadRequestExceptionMapper<NumberFormatException> {}
