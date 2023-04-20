package com.dotcms.rest.api.v1.browsertree;

import com.dotcms.util.TreeableNameComparator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.browser.ajax.BrowserAjax;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by jasontesser on 9/28/16.
 */
public class BrowserTreeHelper {

    private final HostAPI hostAPI;
    private final FolderAPI folderAPI;

    public static final String OPEN_FOLDER_IDS = "siteBrowserOpenFolderIds";
    public static final String ACTIVE_FOLDER_ID = "siteBrowserActiveFolderInode";

    private final static TreeableNameComparator TREEABLE_NAME_COMPARATOR =
            new TreeableNameComparator();

    private BrowserTreeHelper () {
        this.hostAPI = APILocator.getHostAPI();
        this.folderAPI = APILocator.getFolderAPI();
    }

    private static class SingletonHolder {
        private static final BrowserTreeHelper INSTANCE = new BrowserTreeHelper();
    }

    /**
     * Get the instance.
     * @return BrowserTreeHelper
     */
    public static BrowserTreeHelper getInstance() {

        return BrowserTreeHelper.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Return the list of host that the given user has permissions to see.
     *
     * @param siteName Hostname to lookup the URI on
     * @param user
     * @param uri The URI (Path) to get children assets for. If you want the root the Site URI should be set to /
     * @return List of assets that the given user has permissions to see under a URI on a Host. (Currently this only returns Folder and File assets)
     * @throws com.dotmarketing.exception.DotDataException if one is thrown when the sites are search
     * @throws com.dotmarketing.exception.DotSecurityException if one is thrown when the sites are search
     */
    public List<Treeable> getTreeablesUnder(String siteName, User user, String uri) throws DotDataException, DotSecurityException {

        List<Treeable> assets = new ArrayList<>();
        Host host = hostAPI.findByName(siteName,user,false);
        if(uri.equals("/")){
            assets.addAll(host.getChildren(user,false,true,false,false));
        }else{
            Folder folder = folderAPI.findFolderByPath(uri,host,user,false);
            assets.addAll(folder.getChildren(user,false,true,false,false));
        }

        return assets
                .stream().sorted(TREEABLE_NAME_COMPARATOR)
                .collect(Collectors.toList());
    }

    /**
     * Tries to find the select folder
     * @return Optional String, present if the folder select exists
     */
    public Optional<String> findSelectedFolder (final HttpServletRequest request) {

        final BrowserAjax browserAjax = (BrowserAjax)request.getSession().getAttribute("BrowserAjax");
        String siteBrowserActiveFolderInode = (String)request.getSession().getAttribute("siteBrowserActiveFolderInode");
        return Optional.ofNullable(null == siteBrowserActiveFolderInode && null != browserAjax?
                browserAjax.getActiveFolderId(): siteBrowserActiveFolderInode);
    }

    private Optional<Host> findHostFromPath(final String folderPath, final User user) {

        final int hostIndicatorIndex = folderPath.indexOf(HostUtil.HOST_INDICATOR);
        if (-1 != hostIndicatorIndex) {

            final int nextPathSlashIndex = folderPath.indexOf(StringPool.FORWARD_SLASH, hostIndicatorIndex+2);
            if (-1 != nextPathSlashIndex) {

                final String hostname = folderPath.substring(hostIndicatorIndex+2, nextPathSlashIndex);
                final Host   host     = Try.of(()->this.hostAPI.findByName(hostname, user, false)).getOrNull();
                return Optional.ofNullable(host);
            }
        }

        return Optional.empty();
    }

    private String getFolderPath(final String folderPath) {

        final int hostIndicatorIndex = folderPath.indexOf(HostUtil.HOST_INDICATOR);
        if (-1 != hostIndicatorIndex) {

            final int nextPathSlashIndex = folderPath.indexOf(StringPool.FORWARD_SLASH, hostIndicatorIndex+2);
            if (-1 != nextPathSlashIndex) {

                final String path = folderPath.substring(nextPathSlashIndex);
                return path;
            }
        }

        return folderPath;
    }

    /**
     * Sets all the Session parameters that are required for the Site Browser to display a specific folder path in the
     * folder tree. This is useful, for instance, when a User clicks a Container as File and is taken to the exact
     * location where the Container's files live in the Site Browser.
     *
     * @param request              The current {@link HttpServletRequest} instance.
     * @param fullFolderPath       The full path of the folder to select.
     * @param user                 The current {@link User} that is calling this action.
     * @param respectFrontendRoles If {@code true}, permissions will be checked against the User's roles.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The current User does not have the required permissions to perform this action.
     */
    public void selectFolder(final HttpServletRequest request, final String fullFolderPath, final User user,
                             final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        final Optional<Host> siteOpt = this.findHostFromPath(fullFolderPath, user);
        final Host site              = siteOpt.isPresent() ? siteOpt.get() : this.hostAPI.findDefaultHost(user, respectFrontendRoles);
        final Host currentSite       = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        final String folderPath      = this.getFolderPath(fullFolderPath);
        final Folder folder          = this.folderAPI.findFolderByPath(folderPath, site, user, respectFrontendRoles);
        if (null == folder || !UtilMethods.isSet(folder.getIdentifier()) || null == site || !UtilMethods.isSet(site.getIdentifier())) {
            throw new IllegalArgumentException(String.format("Path '%s' is pointing to an invalid Site or an invalid " +
                                                                     "Folder path", fullFolderPath));
        }
        if (null == currentSite || !site.getIdentifier().equals(currentSite.getIdentifier())) {
            HostUtil.switchSite(request, site);
        }
        final HttpSession session = request.getSession();
        session.setAttribute(ACTIVE_FOLDER_ID, folder.getInode());
        final Folder leafFolder = this.folderAPI.find(folder.getInode(), user, false);
        if (null != leafFolder && UtilMethods.isSet(leafFolder.getIdentifier())) {
            Set<String> openFolderIds = (Set<String>) session.getAttribute(OPEN_FOLDER_IDS);
            if (null == openFolderIds) {
                openFolderIds = new HashSet<>();
            }
            session.setAttribute(OPEN_FOLDER_IDS, openFolderIds);
            openFolderIds.add(folder.getInode());
            Permissionable parent = leafFolder.getParentPermissionable();
            while (parent != null) {
                if (parent instanceof Folder) {
                    openFolderIds.add(((Folder) parent).getIdentifier());
                }
                parent = parent.getParentPermissionable();
            }
        }
    }

}
