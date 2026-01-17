package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.exception.BadRequestException;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentletStylingErrorEntity extends ErrorEntity {

    /**
     * Simplified constructor for form-level validation errors. Use this when the error is not
     * specific to a contentlet (e.g., missing container ID).
     *
     * @param errorCode Error code
     * @param message   Error message
     * @param fieldName Field name where error occurred
     */
    public ContentletStylingErrorEntity(String errorCode, String message, String fieldName) {
        super(errorCode, message, fieldName);
    }

    /**
     * Centralized method for throwing BadRequestException with styling errors. This ensures
     * consistent error response format across all styling operations.
     *
     * @param errors List of ErrorEntity objects describing the validation failures
     * @throws BadRequestException Always throws this exception with the provided errors
     */
    public static void throwStylingBadRequest(List<ErrorEntity> errors) {
        throw new BadRequestException(
                null,
                new ResponseEntityView<>(errors),
                "Invalid Style Properties configuration"
        );
    }

    /**
     * Convenience method to throw a BadRequestException with a single form-level validation error.
     * Use this for common single-error scenarios like missing fields or feature flags.
     *
     * @param errorCode Error code
     * @param message   Error message
     * @param fieldName Field name where error occurred (can be null)
     * @throws BadRequestException Always throws with a single styling error
     */
    public static void throwSingleError(String errorCode, String message, String fieldName) {
        final ContentletStylingErrorEntity error =
                new ContentletStylingErrorEntity(errorCode, message, fieldName);
        throwStylingBadRequest(List.of(error));
    }
}
