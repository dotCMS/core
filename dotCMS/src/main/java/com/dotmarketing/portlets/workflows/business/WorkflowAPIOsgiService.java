package com.dotmarketing.portlets.workflows.business;


import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;

public interface WorkflowAPIOsgiService {

	public String addActionlet(Class <? extends WorkFlowActionlet> workFlowActionletClass);

	public void removeActionlet(String workFlowActionletName);

}