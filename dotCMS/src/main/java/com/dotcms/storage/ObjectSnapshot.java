package com.dotcms.storage;

/**
 * One stored object as seen by a single read or listing: its path, value, version and the store's own
 * timestamp. The value and version come from the same response, so the version can be passed to
 * {@link StoragePersistenceAPI#writeObjectIfMatch} to change exactly what was read.
 *
 * @param path     the object's path within its group
 * @param value    the deserialized value, or {@code null} for a listing entry
 * @param version  the store's version tag for this exact content (an S3 ETag)
 * @param modified the store's last-modified time in milliseconds, independent of node clocks
 */
public record ObjectSnapshot(String path, Object value, String version, long modified) { }
