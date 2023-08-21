package com.dotcms.business;

import com.dotmarketing.exception.DotDataException;

import java.util.Map;
import java.util.Optional;

/**
 * Encapsulates the key/value table
 * @author jsanca
 */
public interface SystemTable {

    /**
     * Retrieve a value from the system table by key
     * @param key {@link String}
     * @throws DotDataException
     */
    Optional<String> get(String key);

    /**
     * Retrieve all the values from the system table
     * @return
     * @throws DotDataException
     */
    Map<String, String> all();

    /**
     * Save or Update a value in the system table
     * @param key {@link String} key, should not exist
     * @param value {@link String} value
     * @throws DotDataException
     */
    void set(String key, String value);


    /**
     * Deletes a value from the system table
     * @param key {@link String} key, should exist
     * @throws DotDataException
     */
    void delete(String key);
}
