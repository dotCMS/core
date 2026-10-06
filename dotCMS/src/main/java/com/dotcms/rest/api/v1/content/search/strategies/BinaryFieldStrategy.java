package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import com.dotmarketing.util.LuceneQueryUtils;

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
        final String fieldValue = LuceneQueryUtils.escape(fieldContext.fieldValue().toString());
        // Match against BOTH the analyzed field and its `_dotraw` keyword sub-field (like
        // TextFieldStrategy). The analyzed field tokenizes the file name on hyphens, slashes, dots,
        // etc. (`doc-dev-blue-cold.pdf` -> [doc, dev, blue, cold, pdf]), so a term spanning those
        // separators (`doc-dev-blue-cold`) can only match the un-analyzed `_dotraw` keyword, which
        // stores the whole file name. Without the `_dotraw` clause, only single-token terms match.
        return String.format("+(%s:*%s* %s_dotraw:*%s*)",
                fieldName, fieldValue, fieldName, fieldValue);
    }

}
