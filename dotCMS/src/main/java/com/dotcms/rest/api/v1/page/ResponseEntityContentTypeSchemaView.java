package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for content type style editor schema responses.
 * Contains the {@code DOT_STYLE_EDITOR_SCHEMA} metadata entries keyed by content type variable,
 * for each content type present on a given page.
 */
public class ResponseEntityContentTypeSchemaView extends ResponseEntityView<List<Object>> {
    public ResponseEntityContentTypeSchemaView(final List<Object> styleEditorSchemas) {
        super(styleEditorSchemas);
    }
}