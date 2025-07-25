package com.dotcms.business;

import com.dotmarketing.business.CachableSupport;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides an eager template for a key value table, this eager strategy basically loads all records in cache when any
 * pair is requested, so tries to keeps all records in memory always.
 * @param <K>
 * @param <V>
 */
public interface CacheableEagerFactory<K, V> {

    /**
     * Returns the key used as a flag when the cache is loaded
     * @return K
     */
    K getLoadedKey();

    /**
     * Returns the value used as a flag when the cache is loaded
     * @return V
     */
    V getLoadedValue();

    /**
     * Returns the cache used to store the records
     * @return
     */
    CachableSupport<K, V> getCache();

    /**
     * Returns the SQL used to load all records
     * @return
     */
    String getSelectAllSQL();

    /**
     * Does the save or update operation
     * @param key
     * @param value
     * @throws DotDataException
     */
    void saveOrUpdateInternal(final K key, final V value) throws DotDataException;

    /**
     * Does the delete operation
     * @param key
     * @throws DotDataException
     */
    void deleteInternal(final K key) throws DotDataException;

    /**
     * Returns the key from the record map
     * @param recordMap
     * @return K
     */
    K getKey(Map<String, Object> recordMap);

    /**
     * Returns the value from the record map
     * @param recordMap
     * @return V
     */
    V getValue(Map<String, Object> recordMap);

    /**
     * Wraps the value, does nothing by default but you can override it to do some transformation
     * @param value V
     * @return V
     */
    default V wrap(final V value) {

        return value;
    }

    /**
     * Finds a record by key, returns optional empty if not found
     * this method tries to find the key from the cache, if not found it tries to load all records in cache and then retries
     * @param key
     * @return Optional V
     * @throws DotDataException
     */
    default Optional<V> find(final K key) throws DotDataException {

        V value = null;
        if (UtilMethods.isSet(key)) {

            value = this.getCache().get(key);
            if (Objects.isNull(value) && Objects.isNull((getCache().get(this.getLoadedKey())))) {
                
                // Check if shutdown is in progress - avoid database operations during shutdown
                boolean shutdownInProgress = false;
                try {
                    shutdownInProgress = com.dotcms.shutdown.ShutdownCoordinator.isShutdownStarted();
                } catch (Exception e) {
                    // If we can't check shutdown status, assume not shutting down
                }
                
                if (shutdownInProgress) {
                    // During shutdown, don't attempt database operations that might fail
                    // Return empty result to avoid infinite recursion
                    return Optional.empty();
                }
                
                try {
                    this.findAll();
                    // Only retry if findAll() succeeded - check if cache is now loaded
                    if (Objects.nonNull(getCache().get(this.getLoadedKey()))) {
                        value = this.getCache().get(key);
                    }
                } catch (Exception e) {
                    // If findAll() fails (e.g., database not available), don't retry recursively
                    // This prevents infinite recursion during shutdown or database issues
                    com.dotmarketing.util.Logger.debug(this, "Failed to load all records from database, returning empty result: " + e.getMessage());
                    return Optional.empty();
                }
            }
        }

        return Optional.ofNullable(wrap(value));
    }

    /**
     * Finds all records and loads them into the cache
     * @return Map<K, V>
     * @throws DotDataException
     */
    default Map<K, V> findAll() throws DotDataException {

        // Check if shutdown is in progress - avoid database operations during shutdown
        boolean shutdownInProgress = false;
        try {
            shutdownInProgress = com.dotcms.shutdown.ShutdownCoordinator.isShutdownStarted();
        } catch (Exception e) {
            // If we can't check shutdown status, assume not shutting down
        }
        
        if (shutdownInProgress) {
            // During shutdown, don't attempt database operations that might fail
            // Return empty map to avoid database connectivity issues
            com.dotmarketing.util.Logger.debug(this, "Skipping database operations during shutdown - returning empty result");
            return new HashMap<>();
        }

        final Map<K, V> records = new HashMap<>();

        final List<Map<String, Object>> result = new DotConnect()
                .setSQL(this.getSelectAllSQL())
                .loadObjectResults();

        for (final Map<String, Object> recordMap : result) {

            final K key = getKey(recordMap);
            final V value = getValue(recordMap);
            if (Objects.nonNull(key) && Objects.nonNull(value)) {
                records.put(key, value);
                this.getCache().put(key, value);
            }
        }

        // cache already load
        this.getCache().put(getLoadedKey(), getLoadedValue());
        return records;
    }

    /**
     * Saves or updates a record
     * @param key
     * @param value
     * @throws DotDataException
     */
    default void saveOrUpdate(final K key, final V value) throws DotDataException {

        if (Objects.nonNull(key) && Objects.nonNull(value)) {
            
            // Check if shutdown is in progress - avoid database operations during shutdown
            boolean shutdownInProgress = false;
            try {
                shutdownInProgress = com.dotcms.shutdown.ShutdownCoordinator.isShutdownStarted();
            } catch (Exception e) {
                // If we can't check shutdown status, assume not shutting down
            }
            
            if (shutdownInProgress) {
                // During shutdown, don't attempt database operations that might fail
                com.dotmarketing.util.Logger.debug(this, "Skipping database save/update operation during shutdown");
                throw new DotDataException("Database operations not available during shutdown");
            }

            saveOrUpdateInternal (key, value);

            this.clearCache();
        } else {

            throw new DotDataException("The key and value should not be null");
        }
    }

    /**
     * Deletes a record
     * @param key
     * @throws DotDataException
     */
    default void delete(final K key) throws DotDataException {

        if (Objects.nonNull(key)) {
            
            // Check if shutdown is in progress - avoid database operations during shutdown
            boolean shutdownInProgress = false;
            try {
                shutdownInProgress = com.dotcms.shutdown.ShutdownCoordinator.isShutdownStarted();
            } catch (Exception e) {
                // If we can't check shutdown status, assume not shutting down
            }
            
            if (shutdownInProgress) {
                // During shutdown, don't attempt database operations that might fail
                com.dotmarketing.util.Logger.debug(this, "Skipping database delete operation during shutdown");
                throw new DotDataException("Database operations not available during shutdown");
            }

            deleteInternal(key);
            this.clearCache();
        } else {

            throw new DotDataException("The key should not be null");
        }
    }

    /**
     * Clears the cache
     */
    default void clearCache() {

        this.getCache().clearCache();
    }

}
