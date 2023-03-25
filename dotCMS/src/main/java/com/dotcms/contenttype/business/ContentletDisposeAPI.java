package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

public interface ContentletDisposeAPI {

    /**
     * Massive update on Content-Type and all contents changing the structure-inode to copy-structure meant for disposal
     * This way we can free-up the original structure for re-use quickly
     *
     * @param source
     * @param target
     * @throws DotDataException
     */
    void relocateContentletsForDeletion(ContentType source, ContentType target) throws DotDataException;

    void tearDown(ContentType contentType) throws DotDataException, DotSecurityException;

    void sequentialTearDown(ContentType type) throws DotDataException, DotSecurityException;

}
