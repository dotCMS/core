package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.liferay.util.StringPool.SPACE;

/**
 * This Field Strategy implementation specifies the correct syntax for querying a KeyValue Field via
 * Lucene query in dotCMS.
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
public class KeyValueFieldStrategy implements FieldStrategy {

    private static final String SPECIAL_CHARS_TO_ESCAPE_FOR_META_DATA = "([+\\-!\\(\\){}\\[\\]^\"~?:/\\\\]{2})";
    private static final String VALUE_SPLIT_REGEX = "[,|\\s+]";

    @Override
    public String generateQuery(final FieldContext fieldContext) {
        final String fieldName = fieldContext.fieldName();
        String fieldValue = fieldContext.fieldValue().toString();
        boolean hasQuotes = fieldValue.length() > 1 && fieldValue.endsWith("\"") && fieldValue.startsWith("\"");
        if (hasQuotes) {
            fieldValue = fieldValue.replaceFirst("\"", "");
            fieldValue = fieldValue.substring(0, fieldValue.length()-1);
        }
        final String[] splitter = fieldValue.split(":");
        if (splitter.length > 1) {
            StringBuilder metakey = new StringBuilder();
            for (int x = 0; x < splitter.length - 1; x++) {
                metakey.append(splitter[x]);
            }
            metakey = new StringBuilder(StringUtils.camelCaseLower(metakey.toString()));
            final String metaVal = "*" + splitter[splitter.length - 1] + "*";
            fieldValue = metakey + ":" + metaVal;

            if (fieldName.endsWith("." + FileAssetAPI.META_DATA_FIELD)) {
                return "+" + fieldName + fieldValue.replaceAll(SPECIAL_CHARS_TO_ESCAPE_FOR_META_DATA, "\\\\$1");
            }
        }
        return Arrays.stream(fieldValue.split(VALUE_SPLIT_REGEX))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(token -> String.format("+%s%s:%s%s%s",
                        fieldName, ".key_value", "*", token, "*"))
                .collect(Collectors.joining(SPACE));
    }

}
