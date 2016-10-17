package com.dotmarketing.portlets.rules.conditionlet;

/**
 * 
 * @author Daniel Silva
 * @version 1.0
 * @since 03-25-2015
 *
 */
public class ConditionletInputValue {
    String conditionletInputId;
    String value;

    public ConditionletInputValue(String conditionletInputId, String value) {
    	this.conditionletInputId = conditionletInputId;
    	this.value = value;
    }
    
    public String getConditionletInputId() {
        return conditionletInputId;
    }

    public void setConditionletInputId(String conditionletInputId) {
        this.conditionletInputId = conditionletInputId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

	@Override
	public String toString() {
		return "ConditionletInputValue [conditionletInputId="
				+ conditionletInputId + ", value=" + value + "]";
	}

}
