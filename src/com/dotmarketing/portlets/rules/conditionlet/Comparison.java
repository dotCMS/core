package com.dotmarketing.portlets.rules.conditionlet;

public final class Comparison {

    private String id;
    private String label;

    public Comparison() {}

    public Comparison(String id) {
        this.id = id;
        this.label = id;
    }

    public Comparison(String id, String label) {
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

	@Override
	public String toString() {
		return "Comparison [id=" + id + ", label=" + label + "]";
	}

}
