package com.dotmarketing.osgi.actionlet;

import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;

import java.util.List;
import java.util.Map;

public class MyActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;

    @Override
    public List<WorkflowActionletParameter> getParameters () {
        return null;
    }

    @Override
    public String getName () {
        return "My Osgi Actionlet";
    }

    @Override
    public String getHowTo () {
        return null;
    }

    @Override
    public void executeAction ( WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params ) throws WorkflowActionFailureException {
        System.out.println( "Launch actionlet" );
    }

}