package com.dotcms.ai.workflow;

import com.dotcms.ai.api.AIVisionAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.workflows.actionlet.PublishContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import io.vavr.control.Try;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OpenAIVisionAutoTagActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;
    AIVisionAPI aiVisionAPI = AIVisionAPI.instance.get();

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        return List.of();
    }

    @Override
    public String getName() {
       return "AI - Tag & Alt Descriptions for Images";
    }

    @Override
    public String getHowTo() {
        return "This will attempt to Auto-tag and add alt tag descriptions to your images based on field variables.  You will need to make sure this runs before the save/publish Content actionlet.";
    }


    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
            throws WorkflowActionFailureException {


        Optional<Field> altField = processor.getContentlet().getContentType().fields().stream().filter(f -> f.fieldVariablesMap().containsKey(AIVisionAPI.AI_VISION_ALT_FIELD_VAR)).findFirst();
        Optional<Field> tagField = processor.getContentlet().getContentType().fields().stream().filter(f -> f.fieldVariablesMap().containsKey(AIVisionAPI.AI_VISION_TAG_FIELD_VAR)).findFirst();
        if(altField.isEmpty() && tagField.isEmpty()){
            return;
        }


        String myType = processor.getContentlet().getContentType().variable().toLowerCase();

        Optional<WorkflowActionClass> clazz = Try.of(() ->
                        APILocator.getWorkflowAPI().findActionClasses(processor.getAction())
                                .stream()
                                .filter(ac -> ac.getActionlet() instanceof SaveContentActionlet
                                        || ac.getActionlet() instanceof PublishContentActionlet)
                                .findFirst())
                .getOrElse(Optional.empty());

        if (clazz.isPresent() ) {
            aiVisionAPI.tagImageIfNeeded(processor.getContentlet());
            aiVisionAPI.addAltTextIfNeeded(processor.getContentlet());

        }


    }


}
