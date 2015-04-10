package com.dotmarketing.portlets.rules.model;

import com.dotmarketing.portlets.rules.conditionlet.ConditionletInputValue;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public class Condition {

    public enum Operator {
        AND,
        OR;

        @Override
        public String toString() {
            return super.name();
        }
    }

    private String id;
    private String name;
    private String ruleId;
    private String conditionletId;
    private String conditionGroup;
    private String comparison;
    private List<ConditionValue> values;
    private Date modDate;
    private Operator operator;
    private int priority;

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

    public String getConditionGroup() {
        return conditionGroup;
    }

    public void setConditionGroup(String conditionGroup) {
        this.conditionGroup = conditionGroup;
    }

    public String getComparison() {
        return comparison;
    }

    public void setComparison(String comparison) {
        this.comparison = comparison;
    }

    public List<ConditionValue> getValues() {
        return values;
    }

    public void setValues(List<ConditionValue> values) {
        this.values = values;
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

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
