package com.dotmarketing.servlets.ajax;

class AjaxDirectorServletTestCase {

    private String requestURI;
    private String requestJSON;
    private AjaxDirectorServletInitializer initializer = () -> {};
    private AjaxDirectorServletAssertion assertion;
    private AjaxDirectorServletDisposer disposer;

    String getRequestURI() {
        return requestURI;
    }

    void setRequestURI(final String requestURI) {
        this.requestURI = requestURI;
    }

    String getRequestJSON() {
        return requestJSON;
    }

    void setRequestJSON(final String requestJSON) {
        this.requestJSON = requestJSON;
    }

    AjaxDirectorServletAssertion getAssertion() {
        return assertion;
    }

    void setAssertion(final AjaxDirectorServletAssertion assertion) {
        this.assertion = assertion;
    }

    AjaxDirectorServletDisposer getDisposer() {
        return disposer;
    }

    void setDisposer(final AjaxDirectorServletDisposer disposer) {
        this.disposer = disposer;
    }

    AjaxDirectorServletInitializer getInitializer() {
        return initializer;
    }

    void setInitializer(final AjaxDirectorServletInitializer initializer) {
        this.initializer = initializer;
    }
}
