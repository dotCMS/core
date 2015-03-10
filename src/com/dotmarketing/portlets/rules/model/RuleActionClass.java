package com.dotmarketing.portlets.rules.model;

public class RuleActionClass {
    private String id;
    private String name;
    private String ruleId;
    private int fireOrder;
    private String actionlet;

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

    public int getFireOrder() {
        return fireOrder;
    }

    public void setFireOrder(int fireOrder) {
        this.fireOrder = fireOrder;
    }

    public String getActionlet() {
        return actionlet;
    }

    public void setActionlet(String actionlet) {
        this.actionlet = actionlet;
    }
}
