package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.content.elasticsearch.util.ESUtils;
import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;

import static com.liferay.util.StringPool.COMMA;
import static com.liferay.util.StringPool.SPACE;

/**
 * This Field Strategy implementation specifies the correct syntax for querying a Tag Field via
 * Lucene query in dotCMS.
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
public class TagFieldStrategy implements FieldStrategy {

    @Override
    public String generateQuery(final FieldContext fieldContext) {
        final String fieldName = fieldContext.fieldName();
        final String fieldValue = fieldContext.fieldValue().toString();
        final String[] splitValues = fieldValue.trim().split(COMMA);
        final StringBuilder luceneQuery = new StringBuilder();
        for (final String splitValue : splitValues) {
            final String valueForQuery = ESUtils.escape(splitValue.trim());
            String valueDelimiter = "\"";
            if (valueForQuery.startsWith("\"") && valueForQuery.endsWith("\"")) {
                valueDelimiter = "";
            }
            luceneQuery.append("+").append(fieldName).append(":")
                    .append(valueDelimiter).append(valueForQuery).append(valueDelimiter).append(SPACE);
        }
        return luceneQuery.toString();
    }

}
