package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import com.dotmarketing.util.UtilMethods;

import java.util.Objects;

import static com.dotmarketing.beans.Host.SYSTEM_HOST;
import static com.liferay.util.StringPool.BLANK;

/**
 * This Strategy Field implementation provides a default way to format the value of the Site ID that
 * will be used in the Lucene query, if required. This particular Strategy does not belong to a
 * specific Content Type field, but to the parameter that allows you to specify the Site ID in a
 * Lucene query via the following term:
 * {@link com.dotcms.content.elasticsearch.constants.ESMappingConstants#CONTENTLET_HOST}
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
public class SiteAttributeStrategy implements FieldStrategy {

    @Override
    public boolean checkRequiredValues(final FieldContext fieldContext) {
        return true;
    }

    @Override
    public String generateQuery(final FieldContext queryContext) {
        final String fieldName = queryContext.fieldName();
        final Object fieldValue = queryContext.fieldValue();
        if (Objects.isNull(fieldValue) || UtilMethods.isNotSet(fieldValue.toString())) {
            return BLANK;
        }
        final boolean includeSystemHostContent = (boolean) queryContext.extraParams().getOrDefault("systemHostContent", true);
        final String value = UtilMethods.isSet(fieldValue.toString())
                ? fieldValue.toString()
                : includeSystemHostContent
                    ? SYSTEM_HOST
                    : BLANK;
        final StringBuilder luceneQuery = new StringBuilder();
        if (includeSystemHostContent && UtilMethods.isSet(value) && !value.equals(SYSTEM_HOST)) {
            luceneQuery.append("+(")
                    .append(fieldName).append(":").append(value).append(" ")
                    .append(fieldName).append(":").append(SYSTEM_HOST)
                    .append(")");
        } else if (UtilMethods.isSet(value)) {
            luceneQuery.append("+").append(fieldName).append(":").append(value).append(!value.equals(SYSTEM_HOST) ? "*" : BLANK);
        }
        return luceneQuery.toString();
    }

}
