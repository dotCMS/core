package com.dotmarketing.portlets.workflows.model;

import java.io.Serializable;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;


public class WorkflowActionClass implements Serializable{

	private static final long serialVersionUID = 1L;

	String id;
	String actionId;
	String name;
	int order;
	String clazz;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getActionId() {
		return actionId;
	}
	public void setActionId(String actionId) {
		this.actionId = actionId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getOrder() {
		return order;
	}
	public void setOrder(int order) {
		this.order = order;
	}
	public String getClazz() {
		return clazz;
	}
	public void setClazz(String clazz) {
		this.clazz = clazz;
	}
	@JsonIgnore
	public boolean isNew(){
		return ! UtilMethods.isSet(id);
		
	}
	
	@Override
	public String toString() {
		return "WorkflowActionClass [id=" + id + ", actionId=" + actionId + ", name=" + name + ", order=" + order + ", clazz=" + clazz
				+ "]";
	}

	public WorkFlowActionlet getActionlet() {
		try {
			return APILocator.getWorkflowAPI().newActionlet(clazz);
		} catch (Exception e) {
			Logger.error(WorkflowActionClass.class,e.getMessage(),e);
			throw new WorkflowActionFailureException(e.getMessage());
		} 
	}
	@Override
	public boolean equals(Object obj) {
		if(obj ==null || ! (obj instanceof WorkflowActionClass)) return false;
		return ((WorkflowActionClass)obj).getId().equals(this.getId());
	}
	
	
	
	
}
