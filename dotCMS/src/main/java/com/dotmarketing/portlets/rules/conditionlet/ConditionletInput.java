package com.dotmarketing.portlets.rules.conditionlet;

import java.util.Set;

/**
 * This class contains all the information and/or data needed to build the input for a Condition
 */

public class ConditionletInput {

    private String id;
    private Set<EntryOption> data;
    private boolean userInputAllowed;
    private boolean multipleSelectionAllowed;
    private String defaultValue;
    private Integer minNum;
    private Integer maxNum;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<EntryOption> getData() {
        return data;
    }

    public void setData(Set<EntryOption> data) {
        this.data = data;
    }

    public boolean isUserInputAllowed() {
        return userInputAllowed;
    }

    public void setUserInputAllowed(boolean userInputAllowed) {
        this.userInputAllowed = userInputAllowed;
    }

    public boolean isMultipleSelectionAllowed() {
        return multipleSelectionAllowed;
    }

    public void setMultipleSelectionAllowed(boolean multipleSelectionAllowed) {
        this.multipleSelectionAllowed = multipleSelectionAllowed;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Integer getMinNum() {
        return minNum;
    }

    public void setMinNum(Integer minNum) {
        this.minNum = minNum;
    }

    public Integer getMaxNum() {
        return maxNum;
    }

    public void setMaxNum(Integer maxNum) {
        this.maxNum = maxNum;
    }

	@Override
	public String toString() {
		return "ConditionletInput [id=" + id + ", data=" + data
				+ ", userInputAllowed=" + userInputAllowed
				+ ", multipleSelectionAllowed=" + multipleSelectionAllowed
				+ ", defaultValue=" + defaultValue + ", minNum=" + minNum
				+ ", maxNum=" + maxNum + "]";
	}

}
