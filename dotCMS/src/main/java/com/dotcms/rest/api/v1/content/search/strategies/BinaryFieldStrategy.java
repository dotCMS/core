package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import org.apache.lucene.queryparser.classic.QueryParser;

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
        // Escape Lucene query-syntax characters in the (file-name) term so a hyphen, colon, etc.
        // can't break query parsing; the `*` wildcards we add ourselves stay outside the escape.
        final String fieldValue = QueryParser.escape(fieldContext.fieldValue().toString());
        return "+" + fieldName + ":*" + fieldValue + "*";
    }

}
