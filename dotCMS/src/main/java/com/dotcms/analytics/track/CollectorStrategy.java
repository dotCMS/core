package com.dotcms.analytics.track;

/**
 * A collector command basically puts information into a collector payload bean
 * @author jsanca
 */
public interface CollectorStrategy {

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
     * @return CollectorPayloadBean
     */
    CollectorPayloadBean collect(final CollectorContextMap collectorContextMap, CollectorPayloadBean collectorPayloadBean);

    /**
     * True if the collector should run async
     * @return boolean
     */
    default boolean isAsync() {
        return false;
    }
}
