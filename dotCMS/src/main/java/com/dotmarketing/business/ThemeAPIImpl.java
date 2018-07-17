package com.dotmarketing.business;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.List;


/**
 * Implementation of {@link ThemeAPI}
 */
public class ThemeAPIImpl implements ThemeAPI {

    private ContentletAPI contentletAPI;

    @VisibleForTesting
    ThemeAPIImpl(final ContentletAPI contentletAPI) {
        this.contentletAPI = contentletAPI;
    }

    public ThemeAPIImpl () {
        this(APILocator.getContentletAPI());
    }

    @Override
    public String getThemeThumbnail(final Folder folder, final User user) throws DotSecurityException, DotDataException {
        if (folder == null || user == null) {
            return null;
        }

        final StringBuilder query = new StringBuilder();
        query.append("+conFolder:").append(folder.getInode()).append(" +title:").append(THEME_PNG)
                .append(" +live:true +deleted:false");
        final List<Contentlet> results = contentletAPI.search(query.toString(), -1, 0, null, user, false);

        return UtilMethods.isSet(results) ? results.get(0).getIdentifier() : null;
    }
}
