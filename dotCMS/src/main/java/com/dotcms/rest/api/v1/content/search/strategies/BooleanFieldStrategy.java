package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;

import static org.apache.commons.lang3.BooleanUtils.toBoolean;

/**
 * This Field Strategy implementation specifies the correct syntax for querying a field whose Data
 * Type is True/False -- e.g., a Radio or Select field defined as {@code BOOL} -- via a Lucene query
 * in dotCMS.
 * <p>Such a field is mapped in Elasticsearch as a {@code boolean} (see
 * {@code ESMappingUtilHelper}), which makes the contains-style query produced by the
 * {@link TextFieldStrategy} unusable for it: a wildcard term against a boolean-mapped field is
 * rejected by Elasticsearch, and because dotCMS does not run these queries as lenient, the rejection
 * fails the <b>whole</b> query rather than just that clause. The failure then surfaces as an empty
 * result set with no error at all, so the filter looks like it simply found nothing.</p>
 * <p>Hence the exact term below. The value itself is already normalized to {@code "true"} /
 * {@code "false"} before it reaches here, which is what the index holds: dotCMS coerces a BOOL
 * field's value on save (commons-lang {@code BooleanUtils.toBoolean}), so a field authored with
 * database-style option values such as {@code True|1 / False|0} still stores real booleans.</p>
 *
 * @author dotCMS
 * @since Feb 2026
 */
public class BooleanFieldStrategy implements FieldStrategy {

    @Override
    public String generateQuery(final FieldContext fieldContext) {
        // Coerced with the very function the SAVE path uses to store these values
        // (FieldHandlerStrategyFactory.booleanStrategy), so a filter can never disagree with the
        // contentlet it is meant to match. `Boolean.parseBoolean` would not do: it maps everything that
        // is not the literal "true" to false, so a caller filtering a True/False field by the db-style
        // value its own options are authored with -- `1`, `yes`, `on` -- would silently get the
        // OPPOSITE result set. The Content Drive normalizes before this point, but the generic
        // content-search endpoint routes through this strategy too, with raw values.
        final boolean value = toBoolean(fieldContext.fieldValue().toString().trim());
        return "+" + fieldContext.fieldName() + ":" + value;
    }

}
