package com.dotcms.jitsu;

/**
 * Defines a set of attributes supported in the Analytics Event Payload.
 *
 * <p>This class does not represent the complete set of supported attributes.
 * Instead, it contains only the subset of attributes that require special
 * validation or processing.</p>
 */
public interface ValidAnalyticsEventPayloadAttributes {

    String SESSION_ID_ATTRIBUTE_NAME = "session_id";
    String CONTEXT_ATTRIBUTE_NAME = "context";
    String EVENTS_ATTRIBUTE_NAME = "events";
    String DATA_ATTRIBUTE_NAME = "data";
    String CUSTOM_ATTRIBUTE_NAME = "custom";
    String PAGE_ATTRIBUTE_NAME = "page";
    String DEVICE_ATTRIBUTE_NAME = "device";
    String UTM_ATTRIBUTE_NAME = "utm";

    String SESSION_ID_JITSU_ATTRIBUTE_NAME = "sessionid";

    String URL_ATTRIBUTE_NAME = "url";
    String REFERER_ATTRIBUTE_NAME = "referer";
    String USER_AGENT_ATTRIBUTE_NAME = "user_agent";
    String EVENT_TYPE_ATTRIBUTE_NAME = "event_type";

    String LOCAL_TIME_ATTRIBUTE_NAME = "local_time";

    String SITE_ID_ATTRIBUTE_NAME = "site_id";
}
