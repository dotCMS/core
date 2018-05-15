package com.dotmarketing.portlets.htmlpages.theme.business;

import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import java.util.List;

/**
 * Provides access to routines aimed to interact with information related to contents themes in dotCMS
 */
public interface ThemeAPI {

    /**
     * Return all the themes for user showing the themes of the hostId first
     *
     * @param hostId
     * @return
     */
    List<Contentlet> findAll(User user, String hostId) throws DotSecurityException, DotDataException;

    List<Contentlet> find(User user, String hostId, int limit, int offset, OrderDirection direction)
            throws DotSecurityException, DotDataException;
}
