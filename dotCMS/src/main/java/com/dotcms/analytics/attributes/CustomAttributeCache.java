package com.dotcms.analytics.attributes;

import com.dotcms.analytics.metrics.EventType;
import com.dotmarketing.business.Cachable;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

/**
 * Cache abstraction for custom attribute mappings.
 * <p>
 * Implementations should store and retrieve, per event type, the mapping between a custom attribute
 * key and the database column where it is stored. The cache is also responsible for clearing the
 * group when mappings change.
 */
public interface CustomAttributeCache extends Cachable {

    /**
     * Returns the attribute mapping for the given event type, if present in the cache.
     *
     * @param eventTypeName the event type name.
     * @return a mapping of attribute name to database column, or {@code null} on cache miss.
     */
    Map<String, String> get(String eventTypeName);

    /**
     * Puts the mapping for the provided event type into the cache.
     *
     * @param eventTypeName the event type name.
     * @param attributesMatch the mapping between attribute names and database columns.
     */
    void put (String eventTypeName, Map<String, String> attributesMatch);

}
