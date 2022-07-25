package com.dotcms.rest.api.v1.experiment;

import java.util.HashMap;
import java.util.Map;

public enum AnalyticEventTypeHandlerManager {

    INSTANCE;

    private Map<AnalyticEventType, AnalyticEventTypeHandler> instances;

    AnalyticEventTypeHandlerManager() {
        instances = new HashMap<>();

        instances.put(AnalyticEventType.PAGE_VIEW, new PageViewHandlerAnalyticEventType());
        instances.put(AnalyticEventType.CLICK, new ClickAnalyticEventTypeHandler());
    }

    public AnalyticEventTypeHandler get(AnalyticEventType analyticEventType) {
        return instances.get(analyticEventType);
    }
}
