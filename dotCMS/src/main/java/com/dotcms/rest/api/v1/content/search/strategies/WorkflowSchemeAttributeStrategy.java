package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;

/**
 * This Strategy Field implementation provides a default way to format the value of the Workflow
 * Scheme Attribute that will be used in the Lucene query, if required. This particular Strategy
 * does not belong to a specific Content Type field, but to the parameter that allows you to specify
 * the Workflow Scheme Attribute in a Lucene query via the following term:
 * {@link com.dotcms.content.elasticsearch.constants.ESMappingConstants#WORKFLOW_SCHEME}.
 *
 * @author Jose Castro
 * @since Jan 31st, 2025
 */
public class WorkflowSchemeAttributeStrategy implements FieldStrategy {

    @Override
    public String generateQuery(final FieldContext fieldContext) {
        final String value = (String) fieldContext.fieldValue();
        return "+(" + fieldContext.fieldName() + ":" + value + "*)";
    }

}
