package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.util.UtilMethods;

import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.VARIANT;

/**
 * This Strategy Field implementation provides a default way to format the value of the Variant that
 * will be used in the Lucene query, if required. This particular Strategy does not belong to a
 * specific Content Type field, but to the parameter that allows you to specify the Variant in a
 * Lucene query via the following term:
 * {@link com.dotcms.content.elasticsearch.constants.ESMappingConstants#VARIANT}.
 *
 * @author Jose Castro
 * @since Jan 31st, 2025
 */
public class VariantAttributeStrategy implements FieldStrategy {

    @Override
    public boolean checkRequiredValues(final FieldContext fieldContext) {
        return true;
    }

    @Override
    public String generateQuery(final FieldContext fieldContext) {
        final String value = (String) fieldContext.fieldValue();
        if (UtilMethods.isSet(value) && !VariantAPI.DEFAULT_VARIANT.name().equals(value)) {
            return "+(" + VARIANT + ":" + value + " OR " + VARIANT + ":default)";
        }
        return "+" + VARIANT + ":default";
    }

}
