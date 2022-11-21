package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

public interface SiteAndFolderResolver {

    String CT_FALLBACK_DEFAULT_SITE = "CT_FALLBACK_DEFAULT_SITE";

    ContentType resolveSiteAndFolder(ContentType contentType)
            throws DotDataException, DotSecurityException;

    static SiteAndFolderResolver newInstance(final User user){
        return new SiteAndFolderResolverImpl(user);
    }
}
