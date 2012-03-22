package com.dotmarketing.portlets.workflows.model;

import java.io.Serializable;

public class WorkflowActionletParameter implements Serializable {

	boolean isRequired;
	String displayName;
	String key;
	String defaultValue;
	public boolean isRequired() {
		return isRequired;
	}
	public String getDisplayName() {
		return displayName;
	}
	public String getKey() {
		return key;
	}
	public String getDefaultValue() {
		return defaultValue;
	}
	public WorkflowActionletParameter(String key, String displayName, String defaultValue, boolean isRequired) {
		super();
		this.key = key;
		this.displayName = displayName;
		this.defaultValue = defaultValue;
		this.isRequired = isRequired;
	}
	@Override
	public String toString() {
		return "WorkflowActionletParameter [key=" + key + "]";
	}
	
}
