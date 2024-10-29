package com.dotmarketing.business;

import com.dotcms.config.DotInitializer;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Implementation of {@link ThemeAPI}
 */
public class ThemeAPIImpl implements ThemeAPI, DotInitializer {

    private final FolderAPI folderAPI;
    private final ContentletAPI contentletAPI;
    private Theme systemTheme;
    @VisibleForTesting
    public static final String BASE_LUCENE_QUERY = "+parentpath:/application/themes/* +title:template.vtl ";
    public static final String SYSTEM_THEME_PATH = "/static/system_theme/";
    public static final String SYSTEM_THEME_THUMBNAIL_PATH = "/html/images/system-theme.png";

    @VisibleForTesting
    ThemeAPIImpl(final ContentletAPI contentletAPI, final FolderAPI folderAPI) {
        this.contentletAPI = contentletAPI;
        this.folderAPI = folderAPI;
    }

    public ThemeAPIImpl() {
        this(APILocator.getContentletAPI(), APILocator.getFolderAPI());
    }

    @Override
    public void init() {

        this.initSystemTheme();
    }

    public void initSystemTheme() {
        final String hostIdentifier = APILocator.systemHost().getIdentifier();
        this.systemTheme = new Theme(SYSTEM_THEME_THUMBNAIL_PATH, SYSTEM_THEME_PATH);
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
    public String getThemeThumbnail(final Folder folder, final User user)
            throws DotSecurityException, DotDataException {
        if (folder == null || user == null) {
            return null;
        }

        final List<Contentlet> results = APILocator.getFileAssetAPI().
                findFileAssetsByParentable(folder,null,false,false,user,false)
                .stream().filter(fileAsset -> fileAsset.getFileName().equalsIgnoreCase(THEME_PNG))
                .collect(Collectors.toList());

        return UtilMethods.isSet(results) ? results.get(0).getIdentifier() : null;
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
    public Theme fromFolder(final Folder folder, final User user,
            final boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

        final String themeThumbnail = this.getThemeThumbnail(folder, user);
        final Theme theme = new Theme(themeThumbnail, folder.getPath());
        theme.setIdentifier(folder.getIdentifier());
        theme.setInode(folder.getInode());
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
    public List<Theme> findThemes(final String themeId, final User user, final int limit,
            final int offset, final String hostId, final OrderDirection direction,
            final String searchParams, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        final PaginatedArrayList<Theme> result = new PaginatedArrayList<>();

        //If themeId is sent, it's because a specific theme is wanted, so call the findThemeById
        if (UtilMethods.isSet(themeId)) {
            result.add(findThemeById(themeId, user, respectFrontendRoles));
            return result;
        }

        //TODO: If we modify that the hostId is not required we need to add system theme is hostId is not set.
        //This is because themes can not live under system_host
        if (UtilMethods.isSet(hostId) && hostId.equals(this.systemTheme.getHostId())) {
            result.add(this.systemTheme());
            return result;
        }

        final StringBuilder sqlQuery = new StringBuilder("select cvi.working_inode as inode from contentlet_version_info cvi, identifier id where"
                + " id.parent_path like ? and id.asset_name = ? and cvi.identifier = id.id");
        final List<Object> parameters = new ArrayList<>();
        if (UtilMethods.isSet(searchParams)) {
            parameters.add(Constants.THEME_FOLDER_PATH + StringPool.FORWARD_SLASH +
                    StringPool.PERCENT + searchParams + StringPool.PERCENT);
        }else {
            parameters.add(Constants.THEME_FOLDER_PATH + StringPool.FORWARD_SLASH
                    + StringPool.PERCENT);
        }
        parameters.add(Constants.THEME_META_INFO_FILE_NAME);


        if (UtilMethods.isSet(hostId)) {
            Try.of(() -> APILocator.getHostAPI()
                    .find(hostId, APILocator.systemUser(), false).getIdentifier())
                    .getOrElseThrow(() -> new DoesNotExistException("HostId not belong to any host"));
            Try.of(() -> APILocator.getHostAPI()
                    .find(hostId, user, false).getIdentifier())
                    .getOrElseThrow(() -> new DotSecurityException(
                            "User does not have Permissions over the host"));

            sqlQuery.append(" and id.host_inode = ?");
            parameters.add(hostId);
        }

        //Don't include archived themes (template.vtl archived)
        sqlQuery.append(" and cvi.deleted = ").append(DbConnectionFactory.getDBFalse());

        final String sortBy = String.format("id.parent_path %s", direction.toString().toLowerCase());
        sqlQuery.append(" order by ").append(sortBy);

        final DotConnect dc = new DotConnect().setSQL(sqlQuery.toString())
                .setMaxRows(limit).setStartRow(offset);
        parameters.forEach(dc::addParam);

        //List of inodes of the template.vtl files found
        final List<Map<String,String>> inodesMapList = dc.loadResults();

        final List<String> inodes = new ArrayList<>();
        for (final Map<String, String> versionInfoMap : inodesMapList) {
            inodes.add(versionInfoMap.get("inode"));
        }

        //For each inode found, find the contentlet, get the folder were the contentlet live
        // and convert it to Theme and add to the list
        final List<Contentlet> contentletList  = APILocator.getContentletAPI().findContentlets(inodes);
        for (final Contentlet contentlet : contentletList) {
            final Folder folder = folderAPI.find(contentlet.getFolder(), user, false);
            result.add(this.fromFolder(folder, user, respectFrontendRoles));
        }

        return result;
    }

}
