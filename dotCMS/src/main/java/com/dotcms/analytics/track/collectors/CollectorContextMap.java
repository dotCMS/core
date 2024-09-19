package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.RequestMatcher;

public interface CollectorContextMap {

    Object get(String key);
    RequestMatcher getRequestMatcher(); // since we do not have the previous step phase we need to keep this as an object, but will be a RequestMatcher
}
