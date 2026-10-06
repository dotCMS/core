package com.dotcms.rest.api.v1.asset.bulkduplicate;

import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.util.Logger;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Answers a refused bulk folder duplication with the structured body the contract promises: one
 * {@link ErrorEntity} carrying {@code errorCode}, {@code message} and {@code fieldName}, in the
 * standard error envelope, the same shape bulk folder delete's refusals use (#37062, contract §3).
 * <p>
 * Without it the endpoint would answer {@code 500} to every refusal, because
 * {@link FolderBulkDuplicateRefusedException} is a plain runtime exception the generic mapper does
 * not recognise.
 *
 * @author dotCMS
 */
@Provider
public class FolderBulkDuplicateRefusedExceptionMapper
        implements ExceptionMapper<FolderBulkDuplicateRefusedException> {

    /**
     * Builds the refusal response.
     *
     * @param exception the refusal
     * @return the response with the refusal's status and error body
     */
    @Override
    public Response toResponse(final FolderBulkDuplicateRefusedException exception) {

        // Info, not error: a refused submission is the endpoint working as designed.
        Logger.info(this, String.format("Bulk folder duplicate refused (%s): %s",
                exception.errorCode(), exception.getMessage()));

        final ErrorEntity error = new ErrorEntity(exception.errorCode(), exception.getMessage(),
                exception.fieldName());

        return Response.status(exception.status())
                .entity(new ResponseEntityView<>(List.of(error)))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
