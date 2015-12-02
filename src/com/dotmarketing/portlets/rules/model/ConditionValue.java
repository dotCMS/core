package com.dotmarketing.portlets.rules.model;

import java.io.Serializable;

public class ConditionValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String conditionId;
    private String value;
    private int priority;
    private String key;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getConditionId() {
        return conditionId;
    }

    public void setConditionId(String conditionId) {
        this.conditionId = conditionId;
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
		return "ConditionValue [id=" + id + ", conditionId=" + conditionId
				+ ", value=" + value + ", priority=" + priority + "]";
	}


}
