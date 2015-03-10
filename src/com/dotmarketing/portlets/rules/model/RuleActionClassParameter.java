package com.dotmarketing.portlets.rules.model;

import com.dotcms.repackage.org.codehaus.jackson.annotate.JsonIgnore;
import com.dotmarketing.util.UtilMethods;

import java.io.Serializable;


public class RuleActionClassParameter implements Serializable {

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
	public String getValue() {
		if(UtilMethods.isSet(value)){
			return value;
		}
		else{
			return null;
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
		if(obj ==null || ! (obj instanceof RuleActionClassParameter)) return false;
		return ((RuleActionClassParameter)obj).getId().equals(this.getId());
	}
}
