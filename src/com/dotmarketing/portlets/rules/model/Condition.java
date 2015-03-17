package com.dotmarketing.portlets.rules.model;

import java.util.Date;

public class Condition {

    public enum Operator {
        AND,
        OR
    }

    private String id;
    private String name;
    private String ruleId;
    private String conditionletId;
    private String comparison;
    private String input;
    private Date modDate;
    private Operator operator;

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

    public String getConditionletId() {
        return conditionletId;
    }

    public void setConditionletId(String conditionletId) {
        this.conditionletId = conditionletId;
    }

    public String getComparison() {
        return comparison;
    }

    public void setComparison(String comparison) {
        this.comparison = comparison;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public Date getModDate() {
        return modDate;
    }

    public void setModDate(Date modDate) {
        this.modDate = modDate;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }
}
