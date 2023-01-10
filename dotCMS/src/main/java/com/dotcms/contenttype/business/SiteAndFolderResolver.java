package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;

/**
 * SiteAndFolderResolver API Entry point
 */
public interface SiteAndFolderResolver {

    String CT_SKIP_RESOLVE_SITE = "CT_SKIP_RESOLVE_SITE";

    String CT_FALLBACK_DEFAULT_SITE = "CT_FALLBACK_DEFAULT_SITE";

    ContentType resolveSiteAndFolder(ContentType contentType)
            throws DotDataException, DotSecurityException;

    static SiteAndFolderResolver newInstance(final User user) {
        return new SiteAndFolderResolverImpl(user,
                Config.getBooleanProperty(CT_SKIP_RESOLVE_SITE, false),
                Config.getBooleanProperty(CT_FALLBACK_DEFAULT_SITE, true)
        );
    }
}
