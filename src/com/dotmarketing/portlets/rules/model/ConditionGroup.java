package com.dotmarketing.portlets.rules.model;

public class ConditionGroup {
    private String id;
    private Condition.Operator operator;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Condition.Operator getOperator() {
        return operator;
    }

    public void setOperator(Condition.Operator operator) {
        this.operator = operator;
    }
}
