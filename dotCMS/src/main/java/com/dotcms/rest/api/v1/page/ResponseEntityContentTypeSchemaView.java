package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * Entity View for content type style editor schema responses.
 * Contains the {@code DOT_STYLE_EDITOR_SCHEMA} metadata entries keyed by content type variable,
 * for each content type present on a given page.
 */
public class ResponseEntityContentTypeSchemaView extends ResponseEntityView<List<JsonNode>> {
    public ResponseEntityContentTypeSchemaView(final List<JsonNode> styleEditorSchemas) {
        super(styleEditorSchemas);
    }
}
