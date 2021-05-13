package com.dotcms.system.event.local.model;

/**
 * Intended to provide a key to be used for filtering purposes.
 * e.g. We want to filter an event that can only be received by a group of subscribers
 * Both (The Event and the subscriber) need to provide the same key.
 */
@FunctionalInterface
public interface KeyFilterable {

    Comparable getKey();

}
