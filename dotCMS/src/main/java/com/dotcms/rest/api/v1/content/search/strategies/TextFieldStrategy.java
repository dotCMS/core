package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import com.dotmarketing.util.UtilMethods;
import com.google.common.base.CharMatcher;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.liferay.util.StringPool.SPACE;

/**
 * This Field Strategy implementation specifies the correct syntax for querying a Text Field via
 * Lucene query in dotCMS.
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
public class TextFieldStrategy implements FieldStrategy {

    /** This is the RegEx used to split the values of the field into tokens */
    private static final String VALUE_SPLIT_REGEX = "[,|\\s+]";

    @Override
    public String generateQuery(final FieldContext fieldContext) {
        final String fieldName = fieldContext.fieldName();
        String fieldValue = fieldContext.fieldValue().toString();
        String wildcard = "*";
        if (fieldValue.endsWith("\"") && fieldValue.startsWith("\"")) {
            wildcard = "\"";
            fieldValue = CharMatcher.is('\"').trimFrom(fieldValue).trim();
        }
        final String finalWildcard = wildcard;
        final String luceneQuery;
        if (this.isFieldInURLMapPattern(fieldContext.contentType(), fieldName)) {
            luceneQuery = Arrays.stream(fieldValue.split(VALUE_SPLIT_REGEX))
                    .map(String::trim)
                    .filter(token -> !token.isEmpty())
                    .map(token -> String.format("+%s_dotraw:%s%s%s",
                            fieldName, finalWildcard, token, finalWildcard))
                    .collect(Collectors.joining(SPACE));
        } else {
            luceneQuery = Arrays.stream(fieldValue.split(VALUE_SPLIT_REGEX))
                    .map(String::trim)
                    .filter(token -> !token.isEmpty())
                    .map(token -> String.format("+(%s:%s%s%s %s_dotraw:%s%s%s)",
                            fieldName, finalWildcard, token, finalWildcard,
                            fieldName, finalWildcard, token, finalWildcard))
                    .collect(Collectors.joining(SPACE));
        }
        return luceneQuery;
    }

    /**
     * This method checks if the field is part of the URL Map Pattern of the Content Type it belongs
     * to.
     *
     * @param contentType The Content Type to check.
     * @param fieldName   The field name to check.
     *
     * @return If the field is part of the URL Map patter, returns {@code true}.
     */
    private boolean isFieldInURLMapPattern(final ContentType contentType, final String fieldName) {
        final String fieldVarName = fieldName.substring(fieldName.indexOf(".") + 1);
        return null != contentType && UtilMethods.isSet(contentType.urlMapPattern())
                && contentType.urlMapPattern().contains("{" + fieldVarName + "}");
    }

}
