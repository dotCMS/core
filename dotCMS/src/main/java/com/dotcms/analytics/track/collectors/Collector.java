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

    String CLUSTER = "cluster";
    String COME_FROM_VANITY_URL = "comeFromVanityURL";
    String CONTENT_TYPE_ID = "content_type_id";
    String CONTENT_TYPE_NAME = "content_type_name";
    String CONTENT_TYPE_VAR_NAME = "content_type_var_name";
    String DETAIL_PAGE_URL = "detail_page_url";
    String EVENT_SOURCE = "event_source";
    String EVENT_TYPE = "event_type";
    String HOST = "host";
    String ID = "id";
    String IS_EXPERIMENT_PAGE = "isexperimentpage";
    String IS_TARGET_PAGE = "istargetpage";
    String LANGUAGE = "language";
    String LANGUAGE_ID = "language_id";
    String OBJECT = "object";
    String PERSONA = "persona";
    String REFERER = "referer";
    String RENDER_MODE = "renderMode";
    String REQUEST_ID = "request_id";
    String RESPONSE = "response";
    String RESPONSE_CODE = "response_code";
    String SERVER = "server";
    String SESSION_ID = "sessionId";
    String SESSION_NEW = "sessionNew";
    String SITE = "site";
    String TITLE = "title";
    String URL = "url";
    String USER_AGENT = "userAgent";
    String UTC_TIME = "utc_time";
    String VANITY_QUERY_STRING = "vanity_query_string";

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
