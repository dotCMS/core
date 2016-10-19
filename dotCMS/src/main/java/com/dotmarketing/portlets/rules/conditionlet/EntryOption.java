package com.dotmarketing.portlets.rules.conditionlet;

public final class EntryOption {

    private String id;
    private String label;

    public EntryOption() {}

    public EntryOption(String id) {
        this.id = id;
        this.label = id;
    }

    public EntryOption(String id, String label) {
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
		return "EntryOption [id=" + id + ", label=" + label + "]";
	}

}
