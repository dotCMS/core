package com.dotcms.analytics.track;

import java.io.Serializable;
import java.util.Map;

public interface CollectorContextMap {

    Object get(String key);
    Object getRequestMatcher(); // since we do not have the previous step phase we need to keep this as an object, but will be a RequestMatcher
}
