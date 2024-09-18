package com.dotcms.analytics.track.collectors;

/**
 * A collector command basically puts information into a collector payload bean
 * @author jsanca
 */
public interface Collector {

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
