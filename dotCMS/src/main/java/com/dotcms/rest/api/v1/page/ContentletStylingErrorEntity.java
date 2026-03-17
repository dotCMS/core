package com.dotcms.rest.api.v1.page;

import java.util.List;

import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.exception.BadRequestException;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentletStylingErrorEntity extends ErrorEntity {

    private final String contentletId;
    private final String uuid;
    private final String containerId;

    /**
     * Constructor for creating a ContentletStylingErrorEntity with all fields.
     *
     * @param code         Error code
     * @param message      Error message
     * @param fieldName    Field name where error occurred
     * @param contentletId Contentlet ID
     * @param containerId  Container ID
     * @param uuid         UUID of the container
     */
    public ContentletStylingErrorEntity(String code, String message, String fieldName,
            String contentletId, String containerId, String uuid) {
        super(code, message, fieldName);
        this.contentletId = contentletId;
        this.containerId = containerId;
        this.uuid = uuid;
    }

    /**
     * Simplified constructor for form-level validation errors. Use this when the error is not
     * specific to a contentlet (e.g., missing container ID).
     *
     * @param errorCode Error code
     * @param message   Error message
     * @param fieldName Field name where error occurred
     */
    public ContentletStylingErrorEntity(String errorCode, String message, String fieldName) {
        this(errorCode, message, fieldName, null, null, null);
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
     * Throws a BadRequestException with a single form-level validation error.
     *
     * @param errorCode Error code
     * @param message   Error message
     * @param fieldName Field name where error occurred
     * @throws BadRequestException Always throws with a single styling error
     */
    public static void throwSingleError(String errorCode, String message, String fieldName) {
        final ContentletStylingErrorEntity error =
                new ContentletStylingErrorEntity(errorCode, message, fieldName);
        throwStylingBadRequest(List.of(error));
    }

    /**
     * Throws a BadRequestException with a single form-level error and additional contentlet
     * details.
     *
     * @param errorCode    Error code
     * @param message      Error message
     * @param fieldName    Field name where error occurred
     * @param contentletId Contentlet ID
     * @param containerId  Container ID
     * @param uuid         UUID of the container
     */
    public static void throwSingleError(String errorCode, String message, String fieldName,
            String contentletId, String containerId, String uuid) {
        final ContentletStylingErrorEntity error = new ContentletStylingErrorEntity(errorCode,
                message, fieldName, contentletId, containerId, uuid);
        throwStylingBadRequest(List.of(error));
    }

    public String getContainerId() {
        return containerId;
    }

    public String getUuid() {
        return uuid;
    }

    public String getContentletId() {
        return contentletId;
    }
}
