package com.dotmarketing.portlets.rules.conditionlet.model;

import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;

public class Condition {

    private String id;
    private String name;
    private String ruleId;
    private Conditionlet conditionlet;
    private String operator;
    private String input;

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

    public Conditionlet getConditionlet() {
        return conditionlet;
    }

    public void setConditionlet(Conditionlet conditionlet) {
        this.conditionlet = conditionlet;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }
}
