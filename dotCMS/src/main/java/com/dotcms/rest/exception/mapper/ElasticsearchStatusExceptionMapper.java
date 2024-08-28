package com.dotcms.rest.exception.mapper;

import com.dotmarketing.util.Logger;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;
import org.elasticsearch.ElasticsearchStatusException;
import javax.ws.rs.ext.ExceptionMapper;

@Provider
public class ElasticsearchStatusExceptionMapper implements ExceptionMapper<ElasticsearchStatusException> {


    @Override
    public Response toResponse(final ElasticsearchStatusException exception) {

        //Log into our logs first.
        Logger.warn(this.getClass(), exception.getMessage(), exception);

        //Create the message.
        final String message = exception.getMessage();

        //Creating the message in JSON format.
        final String entity = ExceptionMapperUtil.getJsonErrorAsString(message);

        //Return 4xx message to the client.
        return ExceptionMapperUtil.createResponse(entity, message,
                message.contains("index_not_found_exception") ? Status.NOT_FOUND
                        : Status.INTERNAL_SERVER_ERROR);
    }
}