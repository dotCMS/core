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
     * Generates the appropriate Lucene query for the given field context. By default, this method
     * simply concatenates the field name and value with a colon. No wildcards, escaping or any
     * other special formatting is applied.
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
     * the strategy to be applied. By default, this method simply checks that the field value is not
     * null or empty.
     *
     * @param fieldContext The {@link FieldContext} containing the required information to generate
     *                     the Lucene query.
     *
     * @return If the required values are present, returns {@code true}. Otherwise, the Field
     * Strategy will NOT be executed, and an empty String is returned.
     */
    default boolean checkRequiredValues(final FieldContext fieldContext) {
        final Object fieldValue = fieldContext.fieldValue();
        return null != fieldValue && !fieldValue.toString().isEmpty();
    }

}
