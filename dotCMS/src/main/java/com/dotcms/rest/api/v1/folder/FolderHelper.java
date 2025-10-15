package com.dotcms.rest.api.v1.folder;

import com.dotcms.util.TreeableNameComparator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_CAN_ADD_CHILDREN;
import static com.dotmarketing.util.Constants.DONT_RESPECT_FRONT_END_ROLES;
import static com.liferay.util.StringPool.FORWARD_SLASH;

/**
 * Created by jasontesser on 9/28/16.
 */
public class FolderHelper {

    private static final int SUB_FOLDER_SIZE_DEFAULT_LIMIT = 20;
    private final HostAPI hostAPI;
    private final FolderAPI folderAPI;

    private final static TreeableNameComparator TREEABLE_NAME_COMPARATOR =
            new TreeableNameComparator();

    private FolderHelper() {
        this.hostAPI = APILocator.getHostAPI();
        this.folderAPI = APILocator.getFolderAPI();
    }

    public boolean deleteFolder(final Folder folder, final User user) throws DotDataException, DotSecurityException {

        this.folderAPI.delete(folder, user, false);
        return true;
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

        final List<Folder> savedFolders = new ArrayList<>();
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
        Folder ret;
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
        final String uriParam = !folder.startsWith(FORWARD_SLASH) ? FORWARD_SLASH.concat(folder) : folder;
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

        if(children != null && !children.isEmpty()){
            for(final Folder child : children){
                final FolderView recursiveFolder = getFolders(child, user);
                foldersChildCustoms.add(recursiveFolder);
            }
        }

        return new FolderView(folder,foldersChildCustoms);
    }

    /**
     * This method will return a list of {@link FolderSearchResultView} that lives under
     * the path or starts with the path sent.
     *
     * @param siteId site where the  folders should live under. If is not send will look under all hosts.
     * @param pathToSearch path + folder name to find to
     * @param user user
     * @return list of {@link FolderSearchResultView}
     */
    public List<FolderSearchResultView> findSubFoldersPathByParentPath(final String siteId, final String pathToSearch, final User user)
            throws DotSecurityException, DotDataException {
        final List<FolderSearchResultView> subFolders = new ArrayList<>();

        if(pathToSearch.lastIndexOf(FORWARD_SLASH) == 0){ //If there is only one / we need to search the subfolders under the host(s)
            if(UtilMethods.isSet(siteId)) {
                subFolders.addAll(findSubfoldersUnderHost(siteId,pathToSearch,user));
            } else{
                final List<Host> siteList = APILocator.getHostAPI().findAll(user,false);
                for(final Host site : siteList){
                    if(subFolders.size() < SUB_FOLDER_SIZE_DEFAULT_LIMIT) {
                        subFolders.addAll(findSubfoldersUnderHost(site.getIdentifier(), pathToSearch, user));
                    }
                    continue;
                }
            }

        } else {
            if(UtilMethods.isSet(siteId)) {
                subFolders.addAll(findSubfoldersUnderFolder(siteId,pathToSearch,user));
            } else {
                final List<Host> siteList = APILocator.getHostAPI().findAll(user,false);
                for(final Host site : siteList){
                    if(subFolders.size() < SUB_FOLDER_SIZE_DEFAULT_LIMIT) {
                        subFolders.addAll(findSubfoldersUnderFolder(site.getIdentifier(), pathToSearch, user));
                    }
                    continue;
                }
            }
        }

        return subFolders;
    }

    /**
     * Will find the subfolders living directly under the host passed.
     * If pathToSearch is sent is going to filter the path of the subfolder by it.
     */
    private List<FolderSearchResultView> findSubfoldersUnderHost(final String siteId,
            final String pathToSearch, final User user)
            throws DotSecurityException, DotDataException {
        final List<FolderSearchResultView> subFolders = new ArrayList<>();
        final Host site = APILocator.getHostAPI().find(siteId, user, DONT_RESPECT_FRONT_END_ROLES);

        if (pathToSearch.equals(FORWARD_SLASH)) {
            final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();
            subFolders.add(new FolderSearchResultView(systemFolder.getIdentifier(), systemFolder.getInode(), systemFolder.getPath(), site.getHostname(),
                    Try.of(() -> APILocator.getPermissionAPI().doesUserHavePermission(systemFolder,
                            PERMISSION_CAN_ADD_CHILDREN, user)).getOrElse(false)));
        }

        final List<Folder> subFoldersOfRootPath = APILocator.getFolderAPI().findSubFolders(site, user, DONT_RESPECT_FRONT_END_ROLES);
        subFoldersOfRootPath.stream()
                .filter(folder -> folder.getPath().toLowerCase().startsWith(pathToSearch))
                .limit(SUB_FOLDER_SIZE_DEFAULT_LIMIT)
                .forEach(folder -> subFolders
                        .add(new FolderSearchResultView(folder.getIdentifier(), folder.getInode(), folder.getPath(), site.getHostname(),
                                Try.of(() -> APILocator.getPermissionAPI()
                                        .doesUserHavePermission(folder, PERMISSION_CAN_ADD_CHILDREN, user))
                                        .getOrElse(false))));

        return subFolders;
    }

    /**
     * Will find the subfolders living directly under the host passed and the last valid folder (splitting the pathToSearch by the last '/').
     * And filter the results by what is left after the last '/'.
     */
    private List<FolderSearchResultView> findSubfoldersUnderFolder(final String siteId,
            final String pathToSearch, final User user)
            throws DotSecurityException, DotDataException {
        final List<FolderSearchResultView> subFolders = new ArrayList<>();
        final Host site = APILocator.getHostAPI().find(siteId, user, DONT_RESPECT_FRONT_END_ROLES);

        final int lastIndexOf = pathToSearch.lastIndexOf(FORWARD_SLASH);
        final String lastValidPath = pathToSearch.substring(0, lastIndexOf);
        final Folder lastValidFolder = APILocator.getFolderAPI()
                .findFolderByPath(lastValidPath, site, user, DONT_RESPECT_FRONT_END_ROLES);
        if (UtilMethods.isSet(lastValidFolder) && UtilMethods
                .isSet(lastValidFolder.getInode())) {
            if (pathToSearch.equals(lastValidPath) || pathToSearch.equals(lastValidPath + FORWARD_SLASH)) {
                subFolders.add(new FolderSearchResultView(lastValidFolder.getIdentifier(), lastValidFolder.getInode(), lastValidFolder.getPath(),
                        site.getHostname(),
                        Try.of(() -> APILocator.getPermissionAPI()
                                .doesUserHavePermission(lastValidFolder, PERMISSION_CAN_ADD_CHILDREN, user))
                                .getOrElse(false)));
            }
            final List<Folder> subFoldersOfLastValidPath = APILocator.getFolderAPI().findSubFolders(lastValidFolder, user, DONT_RESPECT_FRONT_END_ROLES);
            subFoldersOfLastValidPath.stream()
                    .filter(folder -> folder.getPath().toLowerCase().startsWith(pathToSearch))
                    .limit(SUB_FOLDER_SIZE_DEFAULT_LIMIT)
                    .forEach(folder -> subFolders
                            .add(new FolderSearchResultView(folder.getIdentifier(), folder.getInode(), folder.getPath(), site.getHostname(),
                                    Try.of(() -> APILocator.getPermissionAPI()
                                            .doesUserHavePermission(folder, PERMISSION_CAN_ADD_CHILDREN, user))
                                            .getOrElse(false))));
        }
        return subFolders;
    }

    /**
     * Check if the folder is valid and has an inode.
     * @param folder folder to check
     * @return true if the folder exists in the database
     */
    public boolean isExistingFolder(final Folder folder) {
        return UtilMethods.isSet(folder) && InodeUtils.isSet(folder.getInode());
    }
}
