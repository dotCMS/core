package com.dotmarketing.portlets.rules.model;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.com.google.common.collect.Maps;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class RuleAction implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String ruleId;
    private int priority;
    private String actionlet;
    private Date modDate;
    private List<RuleActionParameter> parameters = Lists.newArrayList();

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

    public List<RuleActionParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<RuleActionParameter> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(RuleActionParameter parameter){
        this.parameters.add(parameter);
    }

    public Map<String, RuleActionParameter> getParameterMap(){
        Map<String, RuleActionParameter> params = Maps.newHashMap();
        for (RuleActionParameter param : parameters) {
            params.put(param.getKey(), param);
        }
        return params;
    }

    @JsonIgnore
	@Override
	public String toString() {
		return "RuleAction [id=" + id + ", name=" + name + ", ruleId=" + ruleId
				+ ", priority=" + priority + ", actionlet=" + actionlet
				+ ", modDate=" + modDate + "]";
	}

}
