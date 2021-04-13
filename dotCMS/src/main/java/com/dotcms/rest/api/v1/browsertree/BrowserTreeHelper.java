package com.dotcms.rest.api.v1.browsertree;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.UserAPI;
import com.dotcms.util.TreeableNameComparator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.browser.ajax.BrowserAjax;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by jasontesser on 9/28/16.
 */
public class BrowserTreeHelper {

    private final HostAPI hostAPI;
    private final FolderAPI folderAPI;
    private final UserAPI userAPI;

    private final static TreeableNameComparator TREEABLE_NAME_COMPARATOR =
            new TreeableNameComparator();

    private BrowserTreeHelper () {
        this.hostAPI = APILocator.getHostAPI();
        this.folderAPI = APILocator.getFolderAPI();
        this.userAPI = APILocator.getUserAPI();
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
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public Optional<String> findSelectedFolder (final HttpServletRequest request) throws DotDataException, DotSecurityException {

        final BrowserAjax browserAjax = (BrowserAjax)request.getSession().getAttribute("BrowserAjax");
        String siteBrowserActiveFolderInode = (String)request.getSession().getAttribute("siteBrowserActiveFolderInode");
        return Optional.ofNullable(null == siteBrowserActiveFolderInode && null != browserAjax?
                browserAjax.getActiveFolderInode(): siteBrowserActiveFolderInode);
    }
    /**
     * Set the session configuration to select on the site browser a particular folder
     * @param request {@link HttpServletRequest}
     * @param folderPath
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public void selectFolder (final HttpServletRequest request, final String folderPath,
                              final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        final Optional<Host> hostOpt = HostUtil.getHostFromPathOrCurrentHost(folderPath, StringPool.FORWARD_SLASH);
        final Host host        = hostOpt.isPresent()? hostOpt.get(): APILocator.getHostAPI().findDefaultHost(user, respectFrontendRoles);
        final Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        final Folder folder    =  APILocator.getFolderAPI().findFolderByPath(folderPath, host, user, respectFrontendRoles);

        if (null != folder && null != host) {

            if (null == currentHost || !host.getIdentifier().equals(currentHost.getIdentifier())) {

                request.getSession().setAttribute(WebKeys.CURRENT_HOST, host);
                request.getSession().setAttribute(WebKeys.CMS_SELECTED_HOST_ID, host.getIdentifier());
            }

            BrowserAjax browserAjax = (BrowserAjax)request.getSession().getAttribute("BrowserAjax");
            if (null == browserAjax) {

                browserAjax = new BrowserAjax();
                request.getSession().setAttribute("BrowserAjax", browserAjax);
            }

            browserAjax.setCurrentOpenFolder(folder.getInode(), host.getIdentifier(), user);
            request.getSession().setAttribute("siteBrowserActiveFolderInode", folder.getInode());
        } else {

            throw new IllegalArgumentException("The path seems to be not valid: " + folderPath);
        }
    }
}
