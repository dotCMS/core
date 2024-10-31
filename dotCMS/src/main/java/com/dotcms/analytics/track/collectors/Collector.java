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

    String ID = "id";
    String REQUEST_ID = "request_id";
    String UTC_TIME = "utc_time";
    String CLUSTER = "cluster";
    String SERVER = "server";
    String SESSION_ID = "sessionId";
    String SESSION_NEW = "sessionNew";
    String REFERER = "referer";
    String USER_AGENT = "userAgent";
    String PERSONA = "persona";
    String RENDER_MODE = "renderMode";
    String COME_FROM_VANITY_URL = "comeFromVanityURL";
    String IS_EXPERIMENT_PAGE = "isexperimentpage";
    String IS_TARGET_PAGE = "istargetpage";
    String URL = "url";
    String HOST = "host";
    String LANGUAGE = "language";
    String SITE = "site";
    String TITLE = "title";
    String EVENT_TYPE = "event_type";
    String OBJECT = "object";
    String DETAIL_PAGE_URL = "detail_page_url";
    String CONTENT_TYPE_ID = "content_type_id";
    String CONTENT_TYPE_NAME = "content_type_name";
    String CONTENT_TYPE_VAR_NAME = "content_type_var_name";
    String RESPONSE_CODE = "response_code";
    String VANITY_QUERY_STRING = "vanity_query_string";
    String RESPONSE = "response";
    String LANGUAGE_ID = "language_id";
    String EVENT_SOURCE = "event_source";

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
