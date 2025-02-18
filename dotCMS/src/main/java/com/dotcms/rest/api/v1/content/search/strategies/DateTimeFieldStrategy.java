package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;

import static com.liferay.util.StringPool.CLOSE_BRACKET;
import static com.liferay.util.StringPool.OPEN_BRACKET;

/**
 * This Field Strategy implementation specifies the correct syntax for querying a DateTime Field via
 * Lucene query in dotCMS.
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
public class DateTimeFieldStrategy implements FieldStrategy {

    @Override
    public String generateQuery(final FieldContext fieldContext) {
        final String fieldName = fieldContext.fieldName();
        String value = fieldContext.fieldValue().toString();
        if (!(value.contains(OPEN_BRACKET)
                && value.toLowerCase().contains("to")
                && value.contains(CLOSE_BRACKET))) {
            value = OPEN_BRACKET + value + " TO " + value + CLOSE_BRACKET;
        } else if (value.toLowerCase().contains("to")) {
            value = !value.contains(OPEN_BRACKET) ? OPEN_BRACKET + value : value;
            value = !value.contains(CLOSE_BRACKET) ? value + CLOSE_BRACKET : value;
        }
        return "+" + fieldName + ":" + value;
    }
}
