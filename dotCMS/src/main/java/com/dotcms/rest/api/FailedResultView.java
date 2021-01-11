package com.dotcms.rest.api;

public class FailedResultView {

    private final String element;
    private final String errorMessage;

    public FailedResultView(final String element, final String errorMessage) {
        this.element = element;
        this.errorMessage = errorMessage;
    }

    public String getElement() {
        return element;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

}
