package com.dotmarketing.portlets.workflows.business;


public interface WorkflowAPIOsgiService {

	public String addActionlet(Class workFlowActionletClass);

	public void removeActionlet(String workFlowActionletName);

}