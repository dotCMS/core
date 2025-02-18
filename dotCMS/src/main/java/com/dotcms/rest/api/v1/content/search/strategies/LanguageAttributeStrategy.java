package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;

import static com.liferay.util.StringPool.BLANK;

/**
 * This Strategy Field implementation provides a default way to format the value of the Language ID
 * that will be used in the Lucene query, if required. This particular Strategy does not belong to a
 * specific Content Type field, but to the parameter that allows you to specify the Language ID in a
 * Lucene query via the following term:
 * {@link com.dotcms.content.elasticsearch.constants.ESMappingConstants#LANGUAGE_ID}.
 *
 * @author Jose Castro
 * @since Jan 31st, 2025
 */
public class LanguageAttributeStrategy implements FieldStrategy {

    @Override
    public String generateQuery(final FieldContext fieldContext) {
        final int value = (int) fieldContext.fieldValue();
        return value > 0 ? "+" + fieldContext.fieldName() + ":" + value : BLANK;
    }

}
