package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;

/**
 * This Strategy Field implementation provides a default way to format the value of the Archived
 * Content Attribute that will be used in the Lucene query, if required. This particular Strategy
 * does not belong to a specific Content Type field, but to the parameter that allows you to specify
 * if the Lucene query must return {@code archived} contents. It can be set via the following term:
 * {@link com.dotcms.content.elasticsearch.constants.ESMappingConstants#DELETED}.
 *
 * @author Jose Castro
 * @since Jan 31st, 2025
 */
public class ArchivedContentAttributeStrategy implements FieldStrategy {

    @Override
    public boolean checkRequiredValues(final FieldContext fieldContext) {
        return true;
    }

    @Override
    public String generateQuery(final FieldContext fieldContext) {
        final boolean value = Boolean.parseBoolean(fieldContext.fieldValue().toString());
        return "+" + fieldContext.fieldName() + ":" + value;
    }

}
