package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;


/**
 * This API is responsible for deleting all the contentlets associated to a content type
 * @author fabrizzio
 *
 */
public interface ContentTypeDestroyAPI {

    /**
     * Massive update on Content-Type and all contents changing the structure-inode to
     * copy-structure meant for disposal This way we can free-up the original structure for re-use
     * quickly
     *
     * @param source
     * @param target
     * @return
     * @throws DotDataException
     */
    int relocateContentletsForDeletion(ContentType source, ContentType target) throws DotDataException;

    /**
     * Deletes all the contentlets associated to a content type
     * @param contentType
     * @param user
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void destroy(ContentType contentType, User user) throws DotDataException, DotSecurityException;


}
