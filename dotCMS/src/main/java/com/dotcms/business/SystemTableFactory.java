package com.dotcms.business;

import com.dotmarketing.exception.DotDataException;

import java.util.Map;
import java.util.Optional;

/**
 * Encapsulates the persistance class for the System table
 * @author jsanca
 */
public abstract class SystemTableFactory  {

    /**
     * Retrieve a value from the system table by key
     * @param key {@link String}
     * @throws DotDataException
     */
    protected abstract Optional<String> find(String key) throws DotDataException;

    /**
     * Retrieve all the values from the system table
     * @return
     * @throws DotDataException
     */
    protected abstract Map<String, String> findAll() throws DotDataException;

    /**
     * Create a value in the system table
     * @param key {@link String} key, should not exist
     * @param value {@link String} value
     * @throws DotDataException
     */
    protected abstract void save(String key, String value) throws DotDataException;

    /**
     * Update a value in the system table
     * @param key {@link String} key, should exist
     *      * @param value {@link String} value
     * @throws DotDataException
     */
    protected abstract void update(String key, String value) throws DotDataException;

    /**
     * Deletes a value from the system table
     * @param key {@link String} key, should exist
     * @throws DotDataException
     */
    protected abstract void delete(String key) throws DotDataException;

    /**
     * Clear the cache
     */
    abstract protected  void clearCache();
}
