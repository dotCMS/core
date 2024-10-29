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
    String ISEXPERIMENTPAGE = "isexperimentpage";
    String ISTARGETPAGE = "istargetpage";

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
