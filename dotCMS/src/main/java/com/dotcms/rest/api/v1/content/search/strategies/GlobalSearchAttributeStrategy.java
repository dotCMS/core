package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;

/**
 * This Field Strategy implementation specifies the correct syntax for querying a Global Search
 * Field via Lucene query in dotCMS. The global search is exposed as the main search box that users
 * can see in the {@code Search} portlet, the dynamic search dialog in the {@code Relationships}
 * field, and any other portlet that consumes the Lucene Query Builder service. This particular
 * Strategy does not belong to a specific Content Type field, but to the generic way of finding any
 * content that matches the entered value.
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
public class GlobalSearchAttributeStrategy implements FieldStrategy {

    /** This is the RegEx used to split the values of the field into tokens */
    private static final String VALUE_SPLIT_REGEX = "[,|\\s+]";
    private static final String SPECIAL_CHARS_TO_ESCAPE = "([+\\-!\\(\\){}\\[\\]^\"~*?:\\\\]|[&\\|]{2})";

    @Override
    public String generateQuery(final FieldContext fieldContext) {
        final String fieldName = fieldContext.fieldName();
        String value = fieldContext.fieldValue().toString();
        final StringBuilder luceneQuery = new StringBuilder();
        luceneQuery.append("+catchall:").append(value).append("*").append(" ");
        luceneQuery.append(fieldName).append(":'").append(value).append("'^15").append(" ");
        final String[] titleSplit = value.split(VALUE_SPLIT_REGEX);
        if (titleSplit.length > 1) {
            for (final String term : titleSplit) {
                luceneQuery.append(fieldName).append(":").append(term).append("^5").append(" ");
            }
        }
        luceneQuery.append(fieldName).append("_dotraw:*").append(value).append("*^5").append(" ");
        value = value.replaceAll("\\*", "");
        value = value.replaceAll(SPECIAL_CHARS_TO_ESCAPE, "\\\\$1");
        luceneQuery.append("title:").append(value).append("*");
        return luceneQuery.toString();
    }

}
