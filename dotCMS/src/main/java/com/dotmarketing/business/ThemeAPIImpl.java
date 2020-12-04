package com.dotmarketing.business;

import com.dotcms.config.DotInitializer;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.swagger.jersey.listing.ApiListingResourceJSON;

import java.util.List;


/**
 * Implementation of {@link ThemeAPI}
 */
public class ThemeAPIImpl implements ThemeAPI, DotInitializer {

    private ContentletAPI contentletAPI;
    private Theme         systemTheme;
    public static final String SYSTEM_THEME_PATH = "/static/system_theme/template.vtl";

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

    @Override
    public void init() {

        this.initSystemTheme();
    }

    public void initSystemTheme() {

        final String hostIdentifier = APILocator.systemHost().getIdentifier();
        final String themeThumbnail = null;
        this.systemTheme = new Theme(themeThumbnail, SYSTEM_THEME_PATH);
        this.systemTheme.setIdentifier(Theme.SYSTEM_THEME);
        this.systemTheme.setInode(Theme.SYSTEM_THEME);
        this.systemTheme.setHostId(hostIdentifier);
        this.systemTheme.setName("system_theme");
        this.systemTheme.setTitle("System Theme");
    }

    @Override
    public Theme systemTheme() {

        return this.systemTheme;
    }
}
