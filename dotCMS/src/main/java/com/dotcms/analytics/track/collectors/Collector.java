package com.dotcms.analytics.track.collectors;

/**
 * A collector command basically puts information into a collector payload bean. There are different
 * implementations of a Collector, such as:
 * <ul>
 *     <li>{@link BasicProfileCollector}</li>
 *     <li>{@link PagesCollector}</li>
 *     <li>{@link FilesCollector}</li>
 *     <li>{@link SyncVanitiesCollector} and {@link AsyncVanitiesCollector}</li>
 *     <li>And so on</li>
 * </ul>
 * They all retrieve specific information from sources such as the request, the response, or related
 * information form internal APIs, and put that information into a collector payload bean. Such a
 * bean will be sent to the {@link com.dotcms.jitsu.EventLogSubmitter} to be persisted as an event.
 *
 * @author jsanca
 */
public interface Collector {

    String ID = "identifier";
    String TITLE = "title";
    String INODE = "inode"; // NOT USED YET BUT MAY BE DESIRED
    String BASE_TYPE = "baseType";
    String PATH = "path"; // NOT USED YET BUT MAY BE DESIRED
    String LIVE = "live";
    String WORKING = "working";
    String CONTENT_TYPE_VAR_NAME = "contentType";
    String CONTENT_TYPE_NAME = "contentTypeName";
    String CONTENT_TYPE_ID = "contentTypeId";
    String SITE_ID = "conHost";
    String SITE_NAME = "conHostName";
    String LANGUAGE_ID = "languageId";
    String LANGUAGE = "language";

    String EVENT_SOURCE = "event_source";
    String EVENT_TYPE = "event_type";
    String CLUSTER = "cluster";
    String OBJECT = "object";
    String PERSONA = "persona";
    String REFERER = "referer";
    String REQUEST_ID = "request_id";
    String SERVER = "server";
    String SESSION_ID = "sessionId";
    String SESSION_NEW = "sessionNew";
    String URL = "url";
    String USER_AGENT = "userAgent";
    String UTC_TIME = "utc_time";

    String RESPONSE = "action";
    String RESPONSE_CODE = "response_code";

    String DETAIL_PAGE_URL = "detail_page_url";
    String IS_EXPERIMENT_PAGE = "isexperimentpage";
    String IS_TARGET_PAGE = "istargetpage";
    String VANITY_QUERY_STRING = "vanity_query_string";
    String VANITY_URL_KEY = "vanity_url";
    String FORWARD_TO = "forwardTo";

    String EMAIL = "email";
    String USER_OBJECT = "user";

    String CUSTOMER_NAME = "customer_name";
    String CUSTOMER_CATEGORY = "customer_category";
    String ENVIRONMENT_NAME = "environment_name";
    String ENVIRONMENT_VERSION = "environment_version";
    String HTTP_RESPONSE_CODE = "http_response_code";

    /**
     * Test if the collector should run
     * @param collectorContextMap
     * @return
     */
    boolean test(final CollectorContextMap collectorContextMap);
    /**
     * This method is called in order to fire the collector
     * @param collectorContextMap
     * @param collectorPayloadBean
     * @return CollectionCollectorPayloadBean
     */
    CollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                 final CollectorPayloadBean collectorPayloadBean);

    /**
     * True if the collector should run async
     * @return boolean
     */
    default boolean isAsync() {
        return false;
    }

    /**
     * Return an id for the Collector, by default returns the class name.
     * @return
     */
    default String getId() {

        return this.getClass().getName();
    }

    default boolean isEventCreator(){
        return true;
    }
}
