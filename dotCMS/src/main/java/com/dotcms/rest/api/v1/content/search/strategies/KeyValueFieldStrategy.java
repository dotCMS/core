package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.StringUtils;

/**
 * This Field Strategy implementation specifies the correct syntax for querying a KeyValue Field via
 * Lucene query in dotCMS.
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
public class KeyValueFieldStrategy implements FieldStrategy {

    private static final String SPECIAL_CHARS_TO_ESCAPE_FOR_META_DATA = "([+\\-!\\(\\){}\\[\\]^\"~?:/\\\\]{2})";

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
        return "+" + fieldName + ".key_value" + fieldValue.replaceAll(SPECIAL_CHARS_TO_ESCAPE_FOR_META_DATA, "\\\\$1");
    }

}
