package com.dotcms.rest.api.v1.browsertree;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.UserAPI;
import com.dotcms.util.TreeableNameComparator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.List;
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

        List<Treeable> assets = new ArrayList<Treeable>();
        Host host = hostAPI.findByName(siteName,user,true);
        if(uri.equals("/")){
            assets.addAll(host.getChildren(user,true,true,false,true));
        }else{
            Folder folder = folderAPI.findFolderByPath(uri,host,user,true);
            assets.addAll(folder.getChildren(user,true,true,false,true));
        }

        return assets
                .stream().sorted(TREEABLE_NAME_COMPARATOR)
                .collect(Collectors.toList());
    }

}
