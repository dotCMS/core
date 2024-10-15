package com.dotcms.contenttype.business;

import com.dotmarketing.exception.DotDataException;

/**
 * This API allow you to interact with the unique_fields table
 */
public interface UniqueFieldAPI {

    /**
     * Insert a new unique field value, if the value is duplicated then a {@link java.sql.SQLException} is thrown.
     *
     * @param uniqueFieldCriteria
     * @param contentletId
     *
     * @throws UniqueFieldValueDupliacatedException when the Value is duplicated
     * @throws DotDataException when a DotDataException is throws
     */
    void insert(final UniqueFieldCriteria uniqueFieldCriteria, final String contentletId)
            throws UniqueFieldValueDupliacatedException, DotDataException;
}
