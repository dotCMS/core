package com.dotmarketing.portlets.rules.model;

import java.util.Date;

public class ConditionGroup {
    private String id;
    private String ruleId;
    private Condition.Operator operator;
    private Date modDate;
    private int priority;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public Condition.Operator getOperator() {
        return operator;
    }

    public void setOperator(Condition.Operator operator) {
        this.operator = operator;
    }

    public Date getModDate() {
        return modDate;
    }

    public void setModDate(Date modDate) {
        this.modDate = modDate;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

	@Override
	public String toString() {
		return "ConditionGroup [id=" + id + ", ruleId=" + ruleId
				+ ", operator=" + operator + ", modDate=" + modDate
				+ ", priority=" + priority + "]";
	}

}
