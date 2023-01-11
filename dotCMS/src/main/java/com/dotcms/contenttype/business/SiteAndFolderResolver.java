package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.util.ContentTypeImportExportUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.Lazy;

/**
 * SiteAndFolderResolver API Entry point
 */
public interface SiteAndFolderResolver {

    String CT_SKIP_RESOLVE_SITE = "CT_SKIP_RESOLVE_SITE";

    String CT_FALLBACK_DEFAULT_SITE = "CT_FALLBACK_DEFAULT_SITE";

    /**
     * API entry point
     * @param contentType
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    ContentType resolveSiteAndFolder(ContentType contentType)
            throws DotDataException, DotSecurityException;

    /**
     * instance getter
     * @param user
     * @return
     */
    static SiteAndFolderResolver newInstance(final User user) {
        //When getting built from within an import starter context we need to take CT as they are
        //We don't do any CT site resolution etc we simply return whatever has been passed
        //Therefore the way we instantiate our resolver must adjust to the context and situation
        if(isCalledByContentTypeImportExportUtil()){
            return new SiteAndFolderResolverImpl(user, true, true);
        }
        return new SiteAndFolderResolverImpl(user, skipResolveSite.get(), fallbackDefaultSite.get());
    }

    Lazy<Boolean> skipResolveSite = Lazy.of(()->Config.getBooleanProperty(CT_SKIP_RESOLVE_SITE, false));

    Lazy<Boolean> fallbackDefaultSite = Lazy.of(()->Config.getBooleanProperty(CT_FALLBACK_DEFAULT_SITE, true));

    private static boolean isCalledByContentTypeImportExportUtil() {
        return isCalledByClass(ContentTypeImportExportUtil.class.getName());
    }

    @VisibleForTesting
    static boolean isCalledByClass(final String callerClassName) {
        return StackWalker.getInstance().walk(stackFrameStream ->
                stackFrameStream.filter(stackFrame -> callerClassName.equals(stackFrame.getClassName())
                ).findFirst()
        ).isPresent();
    }

}
