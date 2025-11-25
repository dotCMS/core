package com.dotmarketing.portlets.workflows.model;

import java.io.Serializable;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.dotmarketing.util.UtilMethods;


public class WorkflowActionClassParameter implements Serializable {

	private static final long serialVersionUID = 1L;

	String id;
	String actionClassId;
	String key;
	String value;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getActionClassId() {
		return actionClassId;
	}
	public void setActionClassId(String actionClassId) {
		this.actionClassId = actionClassId;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}

	public WorkflowActionClassParameter(){
	}

	public WorkflowActionClassParameter(final String value){
		this.value = value;
	}

	public String getValue() {
		if(UtilMethods.isSet(value)){
			return value;
		}
		else{
			return null;
		}
	}
	 public String getOrDefault(final String defaultValue) {
	    if(UtilMethods.isSet(value)){
	      return value;
	    }
	    else{
	      return defaultValue;
	    }
	  }
	public void setValue(String value) {
		this.value = value;
	}
	@JsonIgnore
	public boolean isNew(){
		return !UtilMethods.isSet(id);
		
	}
	@Override
	public boolean equals(Object obj) {
		if(obj ==null || ! (obj instanceof WorkflowActionClassParameter)) return false;
		return ((WorkflowActionClassParameter)obj).getId().equals(this.getId());
	}
}
