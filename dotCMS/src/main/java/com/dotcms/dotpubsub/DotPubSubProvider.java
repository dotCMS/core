package com.dotcms.dotpubsub;

import com.google.common.annotations.VisibleForTesting;

public interface DotPubSubProvider {

    /**
     * Pass a DotPubSubTopic in listen/respond to events that match your topics key
     * 
     * @param topic
     * @return
     */
    DotPubSubProvider subscribe(DotPubSubTopic topic);

    /**
     * Starts the Provider (if needed)
     * 
     * @return
     */
    DotPubSubProvider start();

    /**
     * Shuts down the Provider (if needed)
     * 
     * @return
     */
    void stop();

    /**
     * Publish an event on a given topic
     * 
     * @param topic
     * @param event
     * @return
     */
    boolean publish(DotPubSubEvent event);

    /**
     * Unsubscribes from a topic
     * 
     * @param topic
     * @return
     */
    DotPubSubProvider unsubscribe(DotPubSubTopic topic);

    /**
     * for testing
     * 
     * @return
     */
    @VisibleForTesting
    default DotPubSubEvent lastEventIn() {
        return null;
    }

    /**
     * for testing
     * 
     * @return
     */
    @VisibleForTesting
    default DotPubSubEvent lastEventOut() {
        return null;
    }

    /**
     * Returns the display name for PubSub Provider
     * @return
     */
    default String getProviderName() {
        return getClass().getSimpleName();
    }

    /**
     * Number of publish attempts for the given topic that this provider knows to have failed,
     * i.e. where {@link #publish(DotPubSubEvent)} ultimately returned {@code false}.
     *
     * Only meaningful for providers that publish asynchronously and therefore cannot report the
     * outcome through their own return value -- {@link QueuingPubSubWrapper} is the one that
     * does, returning {@code true} immediately and completing the real publish on another
     * thread. Callers that publish synchronously already see the {@code boolean} and should
     * count it themselves; they must not add this value on top of their own count for the same
     * attempt, or one failure is counted twice.
     *
     * Counted per topic because a single provider instance is shared by every topic in the JVM
     * (cache invalidation, OSGi restart, cluster management), so a JVM-wide total could not be
     * attributed to the cache transport.
     *
     * The topic key is matched case-insensitively, so a caller may pass either
     * {@link DotPubSubEvent#getTopic()} (already lowercased by the event builder) or
     * {@link DotPubSubTopic#getTopic()} (not normalized) and get the same answer. Implementations
     * that key their own storage by topic must normalize it the same way.
     *
     * @param topic the topic key, as returned by {@link DotPubSubEvent#getTopic()}
     * @return the failure count, or 0 for providers that publish synchronously
     */
    default long getFailedPublishCount(final String topic) {
        return 0;
    }
    
    
    
    
}
