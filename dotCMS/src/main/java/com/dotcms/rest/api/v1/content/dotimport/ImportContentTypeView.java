package com.dotcms.rest.api.v1.content.dotimport;

/**
 * A content type the current user can import content into, as the import form lists it.
 *
 * @param id       the content type's identifier
 * @param variable the content type's velocity variable name
 * @param name     the content type's display name
 * @param baseType the name of the content type's base type, such as {@code CONTENT}
 * @param icon     the content type's icon, when it has one
 */
public record ImportContentTypeView(String id, String variable, String name, String baseType,
        String icon) {
}
