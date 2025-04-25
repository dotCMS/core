package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;

/**
 * This Strategy Field implementation provides a default way to format the value of the Folder ID
 * that will be used in the Lucene query, if required. This particular Strategy does not belong to
 * a specific Content Type field, but to the parameter that allows you to specify the Folder ID in a
 * Lucene query via the following term:
 * {@link com.dotcms.content.elasticsearch.constants.ESMappingConstants#CONTENTLET_FOLDER}
 *
 * @author Jose Castro
 * @since Mar 26th, 2025
 */
public class FolderAttributeStrategy implements FieldStrategy {

    @Override
    public String generateQuery(final FieldContext fieldContext) {
        final String value = (String) fieldContext.fieldValue();
        final String velocityVarName = fieldContext.fieldName();
        return new StringBuilder().append("+").append(velocityVarName).append(":").append(value).append("*").toString();
    }

}
