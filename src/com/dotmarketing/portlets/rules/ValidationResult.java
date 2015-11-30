package com.dotmarketing.portlets.rules;

import java.util.List;

public class ValidationResult {

    private String conditionletInputId;
    private String errorMessage;
    private boolean valid;

    public String getConditionletInputId() {
        return conditionletInputId;
    }

    public void setConditionletInputId(String conditionletInputId) {
        this.conditionletInputId = conditionletInputId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String message, String... messageArgs) {
       this.setErrorMessage(String.format(message == null ? "" : message, (String[])messageArgs));
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

	@Override
	public String toString() {
		return "ValidationResult [conditionletInputId=" + conditionletInputId
				+ ", errorMessage=" + errorMessage + ", valid=" + valid + "]";
	}

}
