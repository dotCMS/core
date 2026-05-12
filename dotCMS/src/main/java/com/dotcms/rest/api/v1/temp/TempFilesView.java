package com.dotcms.rest.api.v1.temp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Schema view for the temp-file endpoints' response body: <code>{ "tempFiles": [DotTempFile, ...] }</code>.
 * Exposes the response shape to OpenAPI clients so generated SDKs and AI agents see a typed
 * <code>tempFiles</code> array instead of an opaque <code>Map&lt;String, Object&gt;</code>.
 *
 * @see TempFileResource
 */
@Schema(description = "Response body for /api/v1/temp and /api/v1/temp/byUrl uploads. " +
        "Contains the tempFiles array; use tempFiles[0].id (e.g. \"temp_5311313004\") " +
        "as the field value when creating contentlets with binary or image fields.")
public class TempFilesView {

    private final List<DotTempFile> tempFiles;

    public TempFilesView(final List<DotTempFile> tempFiles) {
        this.tempFiles = tempFiles;
    }

    public List<DotTempFile> getTempFiles() {
        return tempFiles;
    }
}
