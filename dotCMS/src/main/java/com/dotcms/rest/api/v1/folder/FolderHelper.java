package com.dotcms.rest.api.v1.folder;

import com.dotcms.util.TreeableNameComparator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by jasontesser on 9/28/16.
 */
public class FolderHelper {

    private final HostAPI hostAPI;
    private final FolderAPI folderAPI;

    private final static TreeableNameComparator TREEABLE_NAME_COMPARATOR =
            new TreeableNameComparator();

    private FolderHelper() {
        this.hostAPI = APILocator.getHostAPI();
        this.folderAPI = APILocator.getFolderAPI();
    }

    private static class SingletonHolder {
        private static final FolderHelper INSTANCE = new FolderHelper();
    }

    /**
     * Get the instance.
     *
     * @return FolderHelper
     */
    public static FolderHelper getInstance() {

        return FolderHelper.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Creates all folders and subfolders passed in the list.
     *
     * @param paths List of folders to create
     * @param siteName siteName where the folders are gonna be created
     * @param user user to create folders
     * @return list of folders created
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public List createFolders(final List<String> paths, final String siteName, final User user) throws DotDataException, DotSecurityException {

        final Host host = hostAPI.findByName(siteName, user, true);
        if(!UtilMethods.isSet(host)) {
            throw new IllegalArgumentException(String.format(" Couldn't find any host with name `%s` ",siteName));
        }

        final List<Folder> savedFolders = new ArrayList<Folder>();
        for (final String path : paths) {
            savedFolders.add(folderAPI.createFolders(path, host, user, true));
        }
        return savedFolders.stream().map(this::folderToMap).collect(Collectors.toList());
    }

    private Map folderToMap (final Folder folder) {

        return Try.of(()-> folder.getMap()).getOrElse(Collections.emptyMap());
    }

    /**
     * Return the folder for the passes in URI.
     *
     * @param siteName Folder to lookup the URI on
     * @param user
     * @param uri The URI (Path) to get the folder for. If you pass / will return the SystemFolder
     * @return List of assets that the given user has permissions to see under a URI on a Host. (Currently this only returns Folder and File assets)
     * @throws com.dotmarketing.exception.DotDataException if one is thrown when the sites are search
     * @throws com.dotmarketing.exception.DotSecurityException if one is thrown when the sites are search
     */
    public Folder loadFolderByURI(String siteName, User user, String uri) throws DotDataException, DotSecurityException {
        Host host = hostAPI.findByName(siteName,user,true);
        Folder ret = null;
        if(uri.equals("/")){
            ret = folderAPI.findSystemFolder();
        }else{
            ret = folderAPI.findFolderByPath(uri,host,user,true);
        }
        return ret;
    }

    /**
     *
     * @param hostId hostId where the folder lives
     * @param folder parent folder to find
     * @param user user making the request
     * @return FolderView with the info of the folder requested and the subFolders
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public FolderView loadFolderAndSubFoldersByPath(final String hostId, final String folder, final User user)
            throws DotSecurityException, DotDataException {
        final String uriParam = !folder.startsWith(StringPool.FORWARD_SLASH) ? StringPool.FORWARD_SLASH.concat(folder) : folder;
        final Host host = APILocator.getHostAPI().find(hostId,user,false);
        final Folder folderByPath = APILocator.getFolderAPI().findFolderByPath(uriParam, host, user, false);
        if(!UtilMethods.isSet(host)) {
            throw new IllegalArgumentException(String.format(" Couldn't find any host with id `%s` ",hostId));
        }
        if(!UtilMethods.isSet(folderByPath) || !UtilMethods.isSet(folderByPath.getInode())) {
            throw new IllegalArgumentException(String.format(" Couldn't find any folder with name `%s` in the host `%s`",folder,hostId));
        }

        return getFolders(folderByPath,user);
    }

    /**
     * This method returns a folder structure with their children recursively based on
     * the folder returned by findFolderByPath
     *
     * @param folder  parent folder to  find
     * @param user user
     * @return FolderView a folder structure with their children recursively
     */
    private final FolderView getFolders(final Folder folder, final User user){

        final List<FolderView> foldersChildCustoms = new LinkedList<>();
        List<Folder> children = null;
        try {
            children = APILocator.getFolderAPI().findSubFolders(folder, user, false);
        } catch (Exception e) {
            Logger.error(this, "Error getting findSubFolders for folder "+folder.getPath(), e);
        }

        if(children != null && children.size() != 0){
            for(final Folder child : children){
                final FolderView recursiveFolder = getFolders(child, user);
                foldersChildCustoms.add(recursiveFolder);
            }
        }

        return new FolderView(folder,foldersChildCustoms);
    }

}
