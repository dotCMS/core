package com.dotmarketing.portlets.rules.model;

import java.io.Serializable;

public class ParameterModel implements Serializable {

	private static final long serialVersionUID = 1L;

	private String id;
	private String ownerId;
	private String key;
	private String value;
    private int priority;

    public ParameterModel() {
    }

	public ParameterModel(ParameterModel parameterModelToCopy){
		id = parameterModelToCopy.id;
		ownerId = parameterModelToCopy.ownerId;
		key = parameterModelToCopy.key;
		value = parameterModelToCopy.value;
		priority = parameterModelToCopy.priority;
	}

    public ParameterModel(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

	@Override
	public String toString() {
		return "ParameterModel [id=" + id + ", ownerId=" + ownerId + ", key=" + key + ", value=" + value + ", priority=" + priority + "]";
	}
    
    
    
    
}
