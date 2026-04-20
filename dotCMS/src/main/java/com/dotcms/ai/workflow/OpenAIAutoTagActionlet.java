package com.dotcms.ai.workflow;

import com.dotmarketing.portlets.workflows.actionlet.Actionlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.MultiKeyValue;
import com.dotmarketing.portlets.workflows.model.MultiSelectionWorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;

import java.util.List;
import java.util.Map;

@Actionlet(onlyBatch = true)
public class OpenAIAutoTagActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;

    @Override
    public List<WorkflowActionletParameter> getParameters() {

        WorkflowActionletParameter overwriteParameter = new MultiSelectionWorkflowActionletParameter(OpenAIParams.OVERWRITE_FIELDS.key,
                "Overwrite  tags ", Boolean.toString(true), true,
                () -> List.of(
                        new MultiKeyValue(Boolean.toString(false), Boolean.toString(false)),
                        new MultiKeyValue(Boolean.toString(true), Boolean.toString(true)))
        );

        WorkflowActionletParameter limitTagsToHost = new MultiSelectionWorkflowActionletParameter(
                OpenAIParams.LIMIT_TAGS_TO_HOST.key,
                "Limit the keywords to pre-existing tags", "Limit", false,
                () -> List.of(
                        new MultiKeyValue(Boolean.toString(false), Boolean.toString(false)),
                        new MultiKeyValue(Boolean.toString(true), Boolean.toString(true))
                )
        );

        return List.of(overwriteParameter, limitTagsToHost);
    }

    @Override
    public String getName() {
        return "AI - Auto-Tag Content";
    }

    @Override
    public String getHowTo() {
        return "This will attempt to Auto-tag your content item based on its values ";
    }


    @Override
    public void executeAction(final WorkflowProcessor processor,
                              final Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

        new OpenAIAutoTagRunner(processor, params).run();
    }

}
