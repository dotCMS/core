package com.dotcms.rest.api.v1.asset.bulkdelete;

import javax.ws.rs.core.Response;

/**
 * A bulk-delete submission refused before any job is created (#37063, spec FR-004, FR-005,
 * FR-029; contracts §1).
 * <p>
 * Carries its own {@code errorCode} and HTTP status rather than relying on a generic message,
 * because the two {@code 400} refusals (empty selection, over the maximum) are otherwise
 * indistinguishable to a client — the gap raised on the issue at
 * <a href="https://github.com/dotCMS/core/issues/37063#issuecomment-5720585127">issuecomment-5720585127</a>
 * and resolved in the contract. {@link FolderBulkDeleteRefusedExceptionMapper} is what actually
 * turns this into the {@code {errorCode, message, fieldName}} body the contract promises — a plain
 * {@code RuntimeException} with no registered {@code ExceptionMapper} falls through to {@code 500}
 * for every refusal, which is exactly the bug {@code BulkUploadRefusedExceptionMapper}'s own
 * history records.
 *
 * @author dotCMS
 */
public class FolderBulkDeleteRefusedException extends RuntimeException {

    private final String errorCode;
    private final String fieldName;
    private final Response.Status status;

    public FolderBulkDeleteRefusedException(final String errorCode, final String fieldName,
            final Response.Status status, final String message) {
        super(message);
        this.errorCode = errorCode;
        this.fieldName = fieldName;
        this.status = status;
    }

    /** The stable code a client switches on — e.g. {@code EMPTY_SELECTION}, {@code OVER_MAX_PATHS}. */
    public String errorCode() {
        return errorCode;
    }

    /** Which submitted field is at fault, or {@code null} when the refusal is not field-specific. */
    public String fieldName() {
        return fieldName;
    }

    /** The HTTP status this refusal answers with. */
    public Response.Status status() {
        return status;
    }
}
