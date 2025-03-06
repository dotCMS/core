package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This Field Strategy implementation specifies the correct syntax for querying a Content Types
 * Field via Lucene query in dotCMS.
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
public class ContentTypesFieldStrategy implements FieldStrategy {

    @Override
    public boolean checkRequiredValues(final FieldContext fieldContext) {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String generateQuery(final FieldContext queryContext) {
        final String fieldName = queryContext.fieldName();
        final List<String> fieldValue = (List<String>) queryContext.fieldValue();
        if (Objects.isNull(fieldValue) || fieldValue.isEmpty()) {
            return "+systemType:false -contentType:forms -contentType:Host";
        }
        final StringBuilder luceneQuery = new StringBuilder();
        String value = fieldValue.stream()
                    .map(Object::toString)
                    .map(String::trim)
                    .collect(Collectors.joining(" OR "));
        return luceneQuery.append("+").append(fieldName).append(":(").append(value).append(")").toString();
    }

}
