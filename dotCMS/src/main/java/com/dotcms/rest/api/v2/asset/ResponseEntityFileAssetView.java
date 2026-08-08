package com.dotcms.rest.api.v2.asset;

import com.dotcms.rest.ResponseEntityView;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Concrete {@link ResponseEntityView} wrapper for {@link FileAssetView} used as the OpenAPI
 * {@code @Schema} implementation target so the generated spec reflects the real return type.
 */
@Schema(description = "Response entity wrapping a FileAssetView after a save or publish operation")
public class ResponseEntityFileAssetView extends ResponseEntityView<FileAssetView> {

    public ResponseEntityFileAssetView(final FileAssetView entity) {
        super(entity);
    }
}
