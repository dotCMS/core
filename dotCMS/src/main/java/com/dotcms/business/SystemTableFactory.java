package com.dotcms.business;

import com.dotmarketing.exception.DotDataException;

import java.util.Map;
import java.util.Optional;

/**
 * Encapsulates the persistance class for the System table
 * @author jsanca
 */
public interface SystemTableFactory  {

    /**
     * Retrieve a value from the system table by key
     * @param key {@link String}
     * @throws DotDataException
     */
    Optional<Object> find(String key) throws DotDataException;

    /**
     * Retrieve all the values from the system table
     * @return
     * @throws DotDataException
     */
     Map<String, Object> findAll() throws DotDataException;

    /**
     * Save or Update a value in the system table
     * @param key {@link String} key, should not exist
     * @param value {@link String} value
     * @throws DotDataException
     */
    void saveOrUpdate(String key, Object Object) throws DotDataException;

    /**
     * Deletes a value from the system table
     * @param key {@link String} key, should exist
     * @throws DotDataException
     */
     void delete(String key) throws DotDataException;

    /**
     * Clear the cache
     */
    void clearCache();

    /**
     * Checks if the system table exists, otherwise will create it
     */
    void initIfNeeded();
}
