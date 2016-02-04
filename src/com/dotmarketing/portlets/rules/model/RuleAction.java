package com.dotmarketing.portlets.rules.model;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore;


import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.RuleComponentModel;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.util.Logger;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RuleAction implements RuleComponentModel, Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String ruleId;
    private int priority;
    private String actionlet;
    private Date modDate;
    private List<ParameterModel> parameters;
    private transient RuleActionlet actionDef;
    private transient RuleComponentInstance instance;

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

    public void setParameters(List<ParameterModel> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(ParameterModel parameter) {
        if(parameters==null)
            parameters = new ArrayList<>();

        this.parameters.add(parameter);
    }

    public Map<String, ParameterModel> getParameters(){
    	parameters = (parameters==null) ? new ArrayList<>() : parameters; 
        Map<String, ParameterModel> params = Maps.newHashMap();
        for (ParameterModel param : parameters) {
            params.put(param.getKey(), param);
        }
        return params;
    }

    public void checkValid() {
        this.instance = getActionDefinition().doCheckValid(this);
    }

    public final boolean evaluate(HttpServletRequest req, HttpServletResponse res) {
        //noinspection unchecked
        return getActionDefinition().doEvaluate(req, res, instance);
    }

    public RuleActionlet getActionDefinition(){
        if(actionDef == null) {
            actionDef = APILocator.getRulesAPI().findActionlet(actionlet);
        }
        return actionDef;
    }


	@Override
	public String toString() {
		return "RuleAction [id=" + id + ", name=" + name + ", ruleId=" + ruleId
				+ ", priority=" + priority + ", actionlet=" + actionlet
				+ ", modDate=" + modDate + "]";
	}


}
