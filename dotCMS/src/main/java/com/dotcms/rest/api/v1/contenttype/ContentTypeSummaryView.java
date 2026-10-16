package com.dotcms.rest.api.v1.contenttype;

/**
 * A content type in a short form: enough to name it and pick it in a selector, without its fields
 * or settings. Returned by {@code GET /api/v1/contenttype/_writable} (#37896).
 *
 * @param id       the content type's identifier
 * @param variable the content type's velocity variable name
 * @param name     the content type's display name
 * @param baseType the name of the content type's base type, such as {@code CONTENT}
 * @param icon     the content type's icon, when it has one
 */
public record ContentTypeSummaryView(String id, String variable, String name, String baseType,
        String icon) {
}
