package com.dotcms.rest.exception.mapper;

import com.dotmarketing.util.Logger;
import java.util.Optional;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.OpenSearchException;

@Provider
public class OpenSearchExceptionMapper implements ExceptionMapper<OpenSearchException> {

    @Override
    public Response toResponse(final OpenSearchException exception) {

        Logger.warn(this.getClass(), exception.getMessage(), exception);

        final String message = Optional.ofNullable(exception.error())
                .map(ErrorCause::reason)
                .orElse(exception.getMessage());
        final String entity = ExceptionMapperUtil.getJsonErrorAsString(message);

        final Status status = Status.fromStatusCode(exception.status());
        return ExceptionMapperUtil.createResponse(entity, message,
                status != null ? status : Status.INTERNAL_SERVER_ERROR);
    }
}
