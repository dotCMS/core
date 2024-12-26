package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.RequestMatcher;

public interface CollectorContextMap {

    String REQUEST_ID = "requestId";
    String REQUEST = "request";
    String TIME = "time";
    String CLUSTER = "cluster";
    String SERVER = "server";
    String SESSION = "session";
    String SESSION_NEW = "sessionNew";
    String REFERER =  "referer";
    String USER_AGENT = "user-agent";
    String CURRENT_HOST = "currentHost";
    String LANG_ID = "langId";
    String LANG = "lang";
    String URI = "uri";
    String HOST = "host";
    String EVENT_TYPE = "eventType";
    String PAGE_MODE = "pageMode";

    Object get(String key);
    RequestMatcher getRequestMatcher(); // since we do not have the previous step phase we need to keep this as an object, but will be a RequestMatcher
}
