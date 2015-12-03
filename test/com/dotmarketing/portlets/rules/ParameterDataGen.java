package com.dotmarketing.portlets.rules;

import com.dotmarketing.portlets.rules.model.RuleActionParameter;

/**
 * @author Geoff M. Granum
 */
public class ParameterDataGen {

    private String key = "key";
    private String value = "value";
    private String ownerId;

    public ParameterDataGen() {
    }

    public RuleActionParameter next() {
        RuleActionParameter next = new RuleActionParameter();
        next.setKey(key);
        next.setValue(value);
        next.setRuleActionId(ownerId);
        return next;
    }

    public ParameterDataGen key(String key) {
        this.key = key;
        return this;
    }

    public ParameterDataGen value(String value) {
        this.value = value;
        return this;
    }

    public ParameterDataGen ownerId(String id) {
        this.ownerId = id;
        return this;
    }

}
 
