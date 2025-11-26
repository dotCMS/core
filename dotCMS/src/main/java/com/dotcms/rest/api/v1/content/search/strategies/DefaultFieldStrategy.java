package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;

/**
 * This Strategy Field implementation provides a default way to format the value of a searchable
 * field in a Lucene query, using the simplest valid format without wildcards.
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
public class DefaultFieldStrategy implements FieldStrategy {

    @Override
    public String generateQuery(final FieldContext fieldContext) {
        final String fieldName = fieldContext.fieldName();
        final String fieldValue = fieldContext.fieldValue().toString();
        return "+" + fieldName + ":" + fieldValue;
    }

}
