package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;

/**
 * This Field Strategy implementation specifies the correct syntax for querying a Binary Field via
 * Lucene query in dotCMS.
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
public class BinaryFieldStrategy implements FieldStrategy {

    @Override
    public String generateQuery(final FieldContext fieldContext) {
        final String fieldName = fieldContext.fieldName();
        final String fieldValue = fieldContext.fieldValue().toString();
        return "+" + fieldName + ":*" + fieldValue + "*";
    }

}
