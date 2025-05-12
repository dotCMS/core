package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;

/**
 * This Strategy Field implementation provides a default way to format the value of the Workflow
 * Step Attribute that will be used in the Lucene query, if required. This particular Strategy
 * does not belong to a specific Content Type field, but to the parameter that allows you to specify
 * the Workflow Scheme Attribute in a Lucene query via the following term:
 * {@link com.dotcms.content.elasticsearch.constants.ESMappingConstants#WORKFLOW_STEP}.
 *
 * @author Jose Castro
 * @since Jan 31st, 2025
 */
public class WorkflowStepAttributeStrategy implements FieldStrategy {

    @Override
    public String generateQuery(final FieldContext fieldContext) {
        final String value = (String) fieldContext.fieldValue();
        String velocityVarName = fieldContext.fieldName();
        if (ESMappingConstants.WORKFLOW_CURRENT_STEP_NOT_ASSIGNED_VALUE.equalsIgnoreCase(value)) {
            velocityVarName = ESMappingConstants.WORKFLOW_CURRENT_STEP;
        }
        return "+(" + velocityVarName + ":" + value + "*)";
    }

}
