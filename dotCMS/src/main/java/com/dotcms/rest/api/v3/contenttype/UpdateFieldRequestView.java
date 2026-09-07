package com.dotcms.rest.api.v3.contenttype;

import com.dotcms.rest.api.v1.contenttype.ContentTypeFieldView;
import io.swagger.v3.oas.annotations.media.Schema;

/** Swagger-only view for the wire envelope accepted by the single-field update endpoint. */
@Schema(description = "Request body for updating one content-type field. The field object must be "
        + "wrapped in the top-level `field` property.")
public class UpdateFieldRequestView {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
            description = "The complete field definition to update. Preserve the existing field attributes "
                    + "and change only the intended values; constant fields store their shared value in `values`.")
    private ContentTypeFieldView field;

    public ContentTypeFieldView getField() {
        return field;
    }
}
