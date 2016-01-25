package com.dotmarketing.portlets.rules.util;

import java.io.Serializable;
import java.util.List;

import com.dotmarketing.portlets.rules.model.*;


public class RulesImportExportObject implements Serializable {

	private static final long serialVersionUID = -7961507120261310327L;
	List<Rule> rules;
	List<Condition> conditions;
	List<ConditionGroup> conditionGroups;
	List<RuleAction> ruleActions;
	List<ParameterModel> parameters;
	
	public List<Rule> getRules() {
		return rules;
	}
	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}


	public List<Condition> getConditions() {
		return conditions;
	}
	public void setConditions(List<Condition> conditions) {
		this.conditions = conditions;
	}
	public List<ConditionGroup> getConditionGroups() {
		return conditionGroups;
	}
	public void setConditionGroups(List<ConditionGroup> conditionGroups) {
		this.conditionGroups = conditionGroups;
	}
	public List<RuleAction> getRuleActions() {
		return ruleActions;
	}
	public void setRuleActions(List<RuleAction> ruleActions) {
		this.ruleActions = ruleActions;
	}
	public List<ParameterModel> getParameters() {
		return parameters;
	}
	public void setParameters(List<ParameterModel> parameters) {
		this.parameters = parameters;
	}
	
	
	
	
	
	
	
}
