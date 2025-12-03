package com.dotcms.analytics.attributes;

import com.dotcms.analytics.metrics.EventType;
import com.dotmarketing.exception.DotDataException;

import java.util.Map;

/**
 * Persistence abstraction for analytics custom attribute mappings.
 */
public interface CustomAttributeFactory {

    /**
     * Creates or updates the mapping for the given event type.
     *
     * @param eventTypeName the event type name to persist mappings for.
     * @param attributes the mapping of attribute names to database columns.
     * @throws DotDataException on database access errors.
     */
    void save(String eventTypeName, Map<String, String> attributes) throws DotDataException;

    /**
     * Returns all stored mappings for every event type.
     *
     * @return a map keyed by event type name with their attribute mappings.
     * @throws DotDataException on database access errors.
     */
    Map<String, Map<String, String>> getAll() throws DotDataException;
}
