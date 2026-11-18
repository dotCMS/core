package com.dotcms.rest.api.v1.asset.bulkupload;

import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.util.Logger;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Gives a refused submission the status its own contract promises (contracts §1).
 * <p>
 * <b>Without this the endpoint answered 500 to every ceiling refusal.</b>
 * {@link BulkUploadRefusedException} is a plain {@code RuntimeException}, so JAX-RS routed it to the
 * generic {@code RuntimeExceptionMapper}, whose {@code ResponseUtil.mapExceptionResponse} matches
 * it against {@code BAD_REQUEST_EXCEPTIONS} and the other buckets, finds nothing — the class is in
 * none of them and it carries no cause to match on either — and falls through to
 * {@code INTERNAL_SERVER_ERROR}. An empty batch, a batch over the file count, and a batch over the
 * total size were all answered 500.
 * <p>
 * <b>Which is worse than a wrong number.</b> 500 tells a client the server broke, so a client that
 * retries on server errors will re-upload the whole batch — and be refused again, for the same
 * reason, having spent the bytes twice. The refusal is about the request, and has to say so.
 * <p>
 * <b>The suite could not see this.</b> Every existing test drives {@code BulkUploadHelper} directly
 * and asserts the exception type, which was always correct; the status is decided above the helper,
 * in a layer no test entered. Only the Postman collection asserts the real status, and it was
 * asserting 413 against a 500.
 *
 * @author dotCMS
 */
@Provider
public class BulkUploadRefusedExceptionMapper
        implements javax.ws.rs.ext.ExceptionMapper<BulkUploadRefusedException> {

    @Override
    public Response toResponse(final BulkUploadRefusedException exception) {

        // Logged at info, not error: a refused submission is the endpoint working, not failing.
        // Logging it as an error is how a log stops meaning anything.
        Logger.info(this, String.format("Bulk upload refused (%s): %s",
                exception.ceiling(), exception.getMessage()));

        // The two ceilings stay distinguishable on purpose (FR-004): "too many files" and "too
        // much data" have different fixes, and an author handed one opaque error cannot act on
        // either. REQUEST_ENTITY_TOO_LARGE is 413.
        final Response.Status status =
                BulkUploadRefusedException.Ceiling.TOTAL_SIZE == exception.ceiling()
                        ? Response.Status.REQUEST_ENTITY_TOO_LARGE
                        : Response.Status.BAD_REQUEST;

        return ExceptionMapperUtil.createResponse(exception, status);
    }
}
