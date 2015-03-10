package com.dotmarketing.portlets.rules.conditionlet;

import java.util.List;

public class ValidationResult {

    private String errorMessage;
    private boolean valid;

    public String getErrorMessage() {
        return errorMessage;
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
}
