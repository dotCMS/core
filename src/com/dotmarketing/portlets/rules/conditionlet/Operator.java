package com.dotmarketing.portlets.rules.conditionlet;

public final class Operator {

    private String id;
    private String label;

    public Operator() {}

    public Operator(String id) {
        this.id = id;
        this.label = id;
    }

    public Operator(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
