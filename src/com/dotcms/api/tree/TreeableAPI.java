package com.dotcms.api.tree;

import com.dotcms.rest.api.v1.browsertree.BrowserTreeHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jasontesser on 9/28/16.
 */
public class TreeableAPI {

    private final HostAPI hostAPI;
    private final FolderAPI folderAPI;
    private final UserAPI userAPI;
    private final FileAssetAPI fileAPI;

    /**
     * Get the instance.
     * @return TreeableUtil
     */
    public TreeableAPI() {
        this.hostAPI = APILocator.getHostAPI();
        this.folderAPI = APILocator.getFolderAPI();
        this.userAPI = APILocator.getUserAPI();
        this.fileAPI = APILocator.getFileAssetAPI();
    } // getInstance.

    /**
     *
     * @param host
     * @param user
     * @param live
     * @param working
     * @param archived
     * @param respectFrontEndPermissions
     * @return Will Return the Assets (Currently ONLY Files and Folders) under a Host
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public List<Treeable> loadAssetsUnderHost(Host host, User user, boolean live, boolean working, boolean archived, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
        List<Treeable> assets = new ArrayList<Treeable>();
        assets.addAll(folderAPI.findFoldersByHost(host,user,respectFrontEndPermissions));
        assets.addAll(fileAPI.findFileAssetsByHost(host,user,live,working,archived,respectFrontEndPermissions));
        return assets;
    }

    /**
     *
     * @param folder
     * @param user
     * @param live
     * @param working
     * @param archived NOT currently supported TODO
     * @param respectFrontEndPermissions
     * @return Will Return the Assets (Currently ONLY Files and Folders) under a Folder
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public List<Treeable> loadAssetsUnderFolder(Folder folder, User user,boolean live, boolean working, boolean archived, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
        List<Treeable> assets = new ArrayList<Treeable>();
        assets.addAll(folderAPI.findSubFolders(folder,user,respectFrontEndPermissions));
        assets.addAll(fileAPI.findFileAssetsByFolder(folder,null,live,working,user,respectFrontEndPermissions));
        return assets;
    }

}
