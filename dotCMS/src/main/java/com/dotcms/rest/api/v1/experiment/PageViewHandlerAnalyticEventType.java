package com.dotcms.rest.api.v1.experiment;

import java.util.Map;

public class PageViewHandlerAnalyticEventType implements AnalyticEventTypeHandler{

    private final String JS_CODE  = "console.log('Listener page view events in', window.location);\n"
            + "window.addEventListener('load', (event) => {\n"
            + "  console.log('Tracking page view event');\n"
            + " jitsu('track', 'pageview');\n"
            + "});";

    @Override
    public String getJSCode(final Map<String, String> parameters) {
        return JS_CODE;
    }
}
