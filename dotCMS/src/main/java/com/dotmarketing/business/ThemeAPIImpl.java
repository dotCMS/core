package com.dotmarketing.business;

import com.dotcms.config.DotInitializer;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.PaginationException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.swagger.jersey.listing.ApiListingResourceJSON;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Implementation of {@link ThemeAPI}
 */
public class ThemeAPIImpl implements ThemeAPI, DotInitializer {

    private final FolderAPI     folderAPI;
    private final ContentletAPI contentletAPI;
    private Theme               systemTheme;
    @VisibleForTesting
    public static final String BASE_LUCENE_QUERY           = "+parentpath:/application/themes/* +title:template.vtl ";
    public static final String SYSTEM_THEME_PATH           = "/static/system_theme/";
    public static final String SYSTEM_THEME_THUMBNAIL_PATH = "/html/images/system-theme.png";

    @VisibleForTesting
    ThemeAPIImpl(final ContentletAPI contentletAPI, final FolderAPI folderAPI) {

        this.contentletAPI = contentletAPI;
        this.folderAPI     = folderAPI;
    }

    public ThemeAPIImpl () {
        this(APILocator.getContentletAPI(), APILocator.getFolderAPI());
    }

    @Override
    public void init() {

        this.initSystemTheme();
    }

    public void initSystemTheme() {

        final String hostIdentifier = APILocator.systemHost().getIdentifier();
        this.systemTheme            = new Theme(SYSTEM_THEME_THUMBNAIL_PATH, SYSTEM_THEME_PATH);
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
    public String getThemePath(final Theme theme, final User user, final String themeName,
                               final Host host, final boolean respectAnonPerms) throws DotSecurityException, DotDataException {

        if (theme.isSystemTheme()) {

            return SYSTEM_THEME_PATH;
        }

        final String themeHostId = theme.getHostId();
        return themeHostId.equals(host.getInode())?
                Template.THEMES_PATH + themeName + "/":
                "//" + APILocator.getHostAPI().find(themeHostId, user, respectAnonPerms).getHostname()
                        + Template.THEMES_PATH + themeName + "/";
    }

    @Override
    public Theme findThemeById(final String themeId,
                               final User user,
                               final boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

        if (Theme.SYSTEM_THEME.equalsIgnoreCase(themeId)) {

            return this.systemTheme();
        }

        if (null != themeId) {

            final Folder themeFolder = this.folderAPI.find(themeId, user, respectFrontendRoles);
            if (null != themeFolder && InodeUtils.isSet(themeFolder.getInode())) {

                return this.fromFolder(themeFolder, user, respectFrontendRoles);
            }
        }

        throw new DoesNotExistException("Theme identifier: " + themeId + " does not exists");
    }

    @Override
    public Theme fromFolder(final Folder folder, final User user, final boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

        final String themeThumbnail = this.getThemeThumbnail(folder, user);
        final Theme theme           = new Theme(themeThumbnail, folder.getPath());
        theme.setIdentifier(folder.getIdentifier());
        theme.setInode(theme.getInode());
        theme.setHostId(folder.getHostId());
        theme.setName(folder.getName());
        theme.setTitle(folder.getTitle());
        theme.setFilesMasks(folder.getFilesMasks());
        theme.setShowOnMenu(folder.isShowOnMenu());
        theme.setSortOrder(folder.getSortOrder());
        theme.setDefaultFileType(folder.getDefaultFileType());
        theme.setModDate(folder.getModDate());
        return theme;
    }

    @Override
    public PaginatedArrayList<Theme> findThemes(final String themeId, final User user, final int limit,
                                                final int offset,  final String hostId,  final OrderDirection direction,
                                                final String searchParams,  final boolean respectFrontendRoles) {

        final PaginatedArrayList<Theme> result = new PaginatedArrayList();
        final StringBuilder query = new StringBuilder();

        query.append(BASE_LUCENE_QUERY);

        if (UtilMethods.isSet(themeId)){
            query.append("+conFolder").append(StringPool.COLON).append(themeId);
        }

        if(UtilMethods.isSet(hostId)) {
            query.append("+conhost").append(StringPool.COLON).append(hostId);
        }

        if (UtilMethods.isSet(searchParams)){
            query.append(" +catchall:*").append(searchParams).append("*");
        }

        final String sortBy = String.format("parentPath %s", direction.toString().toLowerCase());

        try {

            final List<ContentletSearch> totalResults =
                    this.contentletAPI.searchIndex(query.toString(), limit, offset, sortBy, user, respectFrontendRoles);

            final List<ContentletSearch> contentletSearches;

            if (limit !=-1 || offset!=0) {

                contentletSearches =
                        this.contentletAPI.searchIndex(query.toString(), limit, offset, sortBy, user, respectFrontendRoles);
                result.setTotalResults(totalResults.size());
            } else {

                contentletSearches = totalResults;
                result.add(this.systemTheme());
                result.setTotalResults(totalResults.size() + 1);
            }

            final List<String>  inodes = contentletSearches.stream()
                    .map(ContentletSearch::getInode).collect(Collectors.toList());

            for (final Contentlet contentlet : this.contentletAPI.findContentlets(inodes)) {

                final Folder folder = folderAPI.find(contentlet.getFolder(), user, false);
                result.add(this.fromFolder(folder, user, respectFrontendRoles));
            }

            result.setQuery(query.toString());
            return result;
        } catch (DotSecurityException | DotDataException e) {

            Logger.error(this, e.getMessage(), e);
            throw new PaginationException(e);
        }
    }
}
