package com.dotcms.rest.api.v1.experiment;

import java.util.Map;

public class AnalyticEvent {

    private Map<String, String> parameters;
    private AnalyticEventType eventKey;

    public AnalyticEvent(final AnalyticEventType eventKey, final Map<String, String> parameters) {
        this.parameters = parameters;
        this.eventKey = eventKey;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public AnalyticEventType getEventKey() {
        return eventKey;
    }
}
