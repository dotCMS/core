package com.dotmarketing.servlets.ajax;

public class AjaxDirectorServletTestCase {

    private String requestURI;
    private String requestJSON;
    private AjaxDirectorServletInitializer initializer = () -> {};
    private AjaxDirectorServletAssertion assertion;
    private AjaxDirectorServletDisposer disposer;

    public String getRequestURI() {
        return requestURI;
    }

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    public String getRequestJSON() {
        return requestJSON;
    }

    public void setRequestJSON(String requestJSON) {
        this.requestJSON = requestJSON;
    }

    public AjaxDirectorServletAssertion getAssertion() {
        return assertion;
    }

    public void setAssertion(AjaxDirectorServletAssertion assertion) {
        this.assertion = assertion;
    }

    public AjaxDirectorServletDisposer getDisposer() {
        return disposer;
    }

    public void setDisposer(AjaxDirectorServletDisposer disposer) {
        this.disposer = disposer;
    }

    public AjaxDirectorServletInitializer getInitializer() {
        return initializer;
    }

    public void setInitializer(AjaxDirectorServletInitializer initializer) {
        this.initializer = initializer;
    }
}
