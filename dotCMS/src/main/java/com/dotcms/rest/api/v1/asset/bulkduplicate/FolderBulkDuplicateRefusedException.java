package com.dotcms.rest.api.v1.asset.bulkduplicate;

import javax.ws.rs.core.Response;

/**
 * A bulk folder duplication submission refused before any run exists (#37062, contract §3).
 * <p>
 * Carries the machine-readable {@code errorCode} and the {@code fieldName} the refusal is about,
 * in the shape bulk folder delete's refusals use, so a client switches on the code rather than the
 * status: an empty selection and one over the maximum are both {@code 400}.
 *
 * @author dotCMS
 */
public class FolderBulkDuplicateRefusedException extends RuntimeException {

    private final String errorCode;
    private final String fieldName;
    private final Response.Status status;

    /**
     * Builds a refusal.
     *
     * @param errorCode the machine-readable reason, such as {@code EMPTY_SELECTION}
     * @param fieldName the request field the refusal is about, or {@code null}
     * @param status    the HTTP status to answer with
     * @param message   diagnostic text for a log; never shown to an author
     */
    public FolderBulkDuplicateRefusedException(final String errorCode, final String fieldName,
            final Response.Status status, final String message) {
        super(message);
        this.errorCode = errorCode;
        this.fieldName = fieldName;
        this.status = status;
    }

    /** @return the machine-readable reason */
    public String errorCode() {
        return errorCode;
    }

    /** @return the request field the refusal is about */
    public String fieldName() {
        return fieldName;
    }

    /** @return the HTTP status to answer with */
    public Response.Status status() {
        return status;
    }
}
