package com.dotcms.rest.api.v1.experiment;

import java.util.Map;

public interface AnalyticEventTypeHandler {

    String getJSCode(final Map<String, String> parameters);

}
