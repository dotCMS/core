package com.dotcms.rest.api.v1.experiment;

import static com.dotmarketing.util.FileUtil.getFileContentFromResourceContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public enum AnalyticEventTypeHandlerManager {

    INSTANCE;

    private Map<AnalyticEventType, String> instances;

    AnalyticEventTypeHandlerManager() {
        instances = new HashMap<>();

        try {
            instances.put(AnalyticEventType.PAGE_VIEW,
                    getFileContentFromResourceContext("/experiment/js/event/page_view_event.js"));
            instances.put(AnalyticEventType.CLICK,
                    getFileContentFromResourceContext("/experiment/js/event/click_event.js"));
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getJsCodeTemplate(AnalyticEventType analyticEventType) {
        return instances.get(analyticEventType);
    }
}
