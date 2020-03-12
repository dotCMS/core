package com.dotcms.rest.exception.mapper;

import com.dotmarketing.util.Logger;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.elasticsearch.ElasticsearchStatusException;
import javax.ws.rs.ext.ExceptionMapper;

public class ElasticsearchStatusExceptionMapper implements ExceptionMapper<ElasticsearchStatusException> {


    @Override
    public Response toResponse(ElasticsearchStatusException exception) {

        //Log into our logs first.
        Logger.warn(this.getClass(), exception.getMessage(), exception);

        //Create the message.
        String message = exception.getMessage();

        //Creating the message in JSON format.
        String entity = ExceptionMapperUtil.getJsonErrorAsString(message);

        //Return 4xx message to the client.
        return ExceptionMapperUtil.createResponse(entity, message,
                message.contains("index_not_found_exception") ? Status.NOT_FOUND
                        : Status.BAD_REQUEST);
    }
}