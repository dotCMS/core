package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;

/**
 * This interface defines the different ways that the value of a searchable field can be formatted
 * to be correctly included in a Lucene query. Whereas some searchable fields can or must be added
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
public interface FieldStrategy {

    /**
     * Generates the appropriate Lucene query for the given field context.
     *
     * @param fieldContext The {@link FieldContext} containing the required information to generate
     *                     the Lucene query.
     *
     * @return The Lucene query for the given field context.
     */
    default String generateQuery(final FieldContext fieldContext) {
        return fieldContext.fieldName() + ":" + fieldContext.fieldValue();
    }

    /**
     * Verifies that the information provided for a given Field Strategy has the required values for
     * the strategy to be applied.
     *
     * @param fieldContext The {@link FieldContext} containing the required information to generate
     *                     the Lucene query.
     *
     * @return If the required values are present, returns {@code true}.
     */
    default boolean checkRequiredValues(final FieldContext fieldContext) {
        final Object fieldValue = fieldContext.fieldValue();
        return null != fieldValue && !fieldValue.toString().isEmpty();
    }

}
