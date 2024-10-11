package com.dotcms.contenttype.business;

import com.dotmarketing.exception.DotDataException;

import java.util.Map;

public interface UniqueFieldFactory {

    /**
     * Insert a new register into the unique_fields table, if already exists another register with the same
     * 'unique_key_val' then a {@link java.sql.SQLException} is thrown.
     *
     * @param key
     * @param supportingValues
     */
    void insert(final String key, final Map<String, Object> supportingValues) throws DotDataException;
}
