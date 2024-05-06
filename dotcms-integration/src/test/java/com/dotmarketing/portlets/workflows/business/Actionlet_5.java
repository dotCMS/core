package com.dotmarketing.portlets.workflows.business;

import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;

import java.util.List;
import java.util.Map;

/**
 * WorkFlowActionlet use on {@link WorkflowAPITest#countAllWorkflowUniqueSubActions()} ()}
 * DO NOT USE IT ON ANOTHER TEST
 */
public class Actionlet_5 extends WorkFlowActionlet {

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        return null;
    }

    @Override
    public String getName() {
        return "Actionlet_5";
    }

    @Override
    public String getHowTo() {
        return null;
    }

    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

    }
}
