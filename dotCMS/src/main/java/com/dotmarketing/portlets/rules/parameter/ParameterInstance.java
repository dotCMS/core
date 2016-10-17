package com.dotmarketing.portlets.rules.parameter;

public class ParameterInstance {

    private final ParameterDefinition definition;
	private final String key;
	private final String value;

    public ParameterInstance(String key, String value, ParameterDefinition definition) {
        this.key = key;
        this.value = value;
        this.definition = definition;
    }

	public String getKey() {
		return key;
	}

    public String getValue() {
		return value;
	}

    public ParameterDefinition getDefinition() {
        return definition;
    }

    public void checkValid() {
        this.definition.getInputType().getDataType().checkValid(getValue());
    }
}
