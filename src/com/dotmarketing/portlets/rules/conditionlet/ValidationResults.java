package com.dotmarketing.portlets.rules.conditionlet;

import java.util.List;

public class ValidationResults {

    private boolean errors;

    private List<ValidationResult> results;

    public boolean hasErrors() {
        return errors;
    }

    public void setErrors(boolean errors) {
        this.errors = errors;
    }

    public List<ValidationResult> getResults() {
        return results;
    }

    public void setResults(List<ValidationResult> results) {
        this.results = results;
    }

	@Override
	public String toString() {
		return "ValidationResults [errors=" + errors + ", results=" + results
				+ "]";
	}

}
