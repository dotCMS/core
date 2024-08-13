package com.dotcms.rest.exception.mapper.badrequest;
import com.amazonaws.services.kms.model.AlreadyExistsException;
import com.dotcms.rest.exception.mapper.DotBadRequestExceptionMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import javax.ws.rs.ext.Provider;

@Provider
public class AlreadyExistsExceptionMapper
        extends DotBadRequestExceptionMapper<AlreadyExistsException> {
}
