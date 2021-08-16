package com.dotcms.content.business;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.util.List;

/**
 * This API provides useful methods and mechanisms to map properties that are present in a {@link Contentlet} object,
 * specially for ES indexation purposes.
 *
 * @author root
 * @since Mar 22nd, 2012
 */
public interface ContentMappingAPI {

	boolean RESPECT_FRONTEND_ROLES = Boolean.TRUE;
    boolean DONT_RESPECT_FRONTEND_ROLES = Boolean.FALSE;

    List<String> dependenciesLeftToReindex(Contentlet con)
            throws DotStateException, DotDataException, DotSecurityException;

}
