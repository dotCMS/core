package com.dotmarketing.portlets.rules.model;

import java.util.Date;

import com.dotcms.repackage.org.codehaus.jackson.annotate.JsonIgnore;

public class RuleAction {
    private String id;
    private String name;
    private String ruleId;
    private int priority;
    private String actionlet;
    private Date modDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getActionlet() {
        return actionlet;
    }

    public void setActionlet(String actionlet) {
        this.actionlet = actionlet;
    }

    public Date getModDate() {
        return modDate;
    }

    public void setModDate(Date modDate) {
        this.modDate = modDate;
    }

    @JsonIgnore
	@Override
	public String toString() {
		return "RuleAction [id=" + id + ", name=" + name + ", ruleId=" + ruleId
				+ ", priority=" + priority + ", actionlet=" + actionlet
				+ ", modDate=" + modDate + "]";
	}

}
