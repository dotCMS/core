package com.dotcms.rest.api.v1.asset.bulkdelete;

import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.util.Logger;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Gives a refused bulk-delete submission the structured body its own contract promises — a single
 * {@link ErrorEntity} (#37063, contracts §1), carried in dotCMS's own standard error envelope,
 * {@link ResponseEntityView#ResponseEntityView(List)}. **Not a bespoke shape**: it is the same
 * envelope every successful response already uses (via its {@code entity} field), and
 * {@code ExceptionMapperUtil.createResponse(Status, DotContentletValidationException)} already
 * builds an error response from it the same way — confirmed 2026-09-19 while reconciling with the
 * frontend half (PR dotCMS/core#37612), which had independently assumed this exact envelope rather
 * than a one-off shape.
 * <p>
 * <b>Without this the endpoint would answer {@code 500} to every refusal.</b>
 * {@link FolderBulkDeleteRefusedException} is a plain {@code RuntimeException}; JAX-RS routes an
 * unmapped one to the generic runtime mapper, which falls through to
 * {@code INTERNAL_SERVER_ERROR} for a class it does not recognize.
 * {@code BulkUploadRefusedExceptionMapper}'s own header comment records exactly this defect
 * shipping once already, caught only by its Postman collection — not by tests that drive the
 * helper directly. See {@code FolderBulkDeleteResourceIT}'s note on the same finding.
 *
 * @author dotCMS
 */
@Provider
public class FolderBulkDeleteRefusedExceptionMapper
        implements ExceptionMapper<FolderBulkDeleteRefusedException> {

    @Override
    public Response toResponse(final FolderBulkDeleteRefusedException exception) {

        // Logged at info, not error: a refused submission is the endpoint working as designed.
        Logger.info(this, String.format("Bulk folder delete refused (%s): %s",
                exception.errorCode(), exception.getMessage()));

        final ErrorEntity error = new ErrorEntity(exception.errorCode(), exception.getMessage(),
                exception.fieldName());

        return Response.status(exception.status())
                .entity(new ResponseEntityView<>(List.of(error)))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
