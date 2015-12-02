package com.dotmarketing.portlets.rules.model;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore;
import com.dotmarketing.util.UtilMethods;

import java.io.Serializable;


public class RuleActionParameter implements Serializable {

	private static final long serialVersionUID = 1L;

	private String id;
	private String ruleActionId;
	private String key;
	private String value;

    public RuleActionParameter() {
    }

    public RuleActionParameter(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getRuleActionId() {
		return ruleActionId;
	}
	public void setRuleActionId(String ruleActionId) {
		this.ruleActionId = ruleActionId;
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
	@JsonIgnore
	public boolean isNew(){
		return id == null;
		
	}
	@Override
	public boolean equals(Object obj) {
		if(obj ==null || ! (obj instanceof RuleActionParameter)) return false;
		return ((RuleActionParameter)obj).getId().equals(this.getId());
	}

	@JsonIgnore
	@Override
	public String toString() {
		return "RuleActionParameter [id=" + id + ", ruleActionId="
				+ ruleActionId + ", key=" + key + ", value=" + value + "]";
	}

}
