package com.dotmarketing.portlets.contentlet.transform;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builder to gain access to the Folder Transformer
 */
public class DotFolderTransformerBuilder {

    private final FolderAPI folderAPI = APILocator.getFolderAPI();

    private boolean defaultView = false;
    private User user;
    private Role[] roles;
    private List<String> folderIds;
    private List<Folder> folders;

    /**
     * Folders can be specified with ids or folder objects
     * @param folders
     * @return
     */
    public DotFolderTransformerBuilder withFolders(final Folder... folders){
       this.folders = Arrays.asList(folders);
       return this;
    }

    /**
     * Folders can be specified with ids or folder objects
     * @param folders
     * @return
     */
    public DotFolderTransformerBuilder withFolders(final List<Folder> folders){
        this.folders = folders;
        return this;
    }

    /**
     * Folders can be specified with ids or folder objects
     * @param folderIds
     * @return
     */
    public DotFolderTransformerBuilder withFolders(final String... folderIds){
        this.folderIds = Arrays.asList(folderIds);
        return this;
    }

    /**
     * Submit user and roles required to build the SiteBrowse view
     * @param user
     * @param roles
     * @return
     */
    public DotFolderTransformerBuilder withUserAndRoles(final User user, final Role... roles){
        this.user = user;
        this.roles = Arrays.copyOf(roles, roles.length);
        return this;
    }

    /**
     * Content-Drive is the default view for Folders
     * @param user
     * @param roles
     * @return
     */
    public DotFolderTransformerBuilder withDefaultView(final User user, final Role... roles){
        this.user = user;
        this.roles = Arrays.copyOf(roles, roles.length);
        this.defaultView = true;
        return this;
    }

    /**
     * Given the different param This Will get you the instance of  DotMapViewTransformer
     * @return
     */
    public DotMapViewTransformer build() {
        List<Folder> resolvedFolders = this.folders;
        if (null == resolvedFolders) {
            resolvedFolders = resolveFoldersFromIds(folderIds);
        }
        //Content-Drive Default View
        if (defaultView && null != user && roles != null) {
            return DotFolderTransformerImpl.defaultInstance(user, roles, resolvedFolders);
        }
        if (null != user && roles != null) {
            return new DotFolderTransformerImpl(user, roles, resolvedFolders);
        }
        return new DotFolderTransformerImpl(resolvedFolders);
    }

    /**
     * Given a list of ids this will get you a List of the respective Folders
     * @param folderIds
     * @return
     */
    private List<Folder> resolveFoldersFromIds(final List<String>folderIds) {
        final User user = APILocator.systemUser();
        return folderIds.stream().map(folderId -> {
            try {
                return folderAPI.find(folderId, user, true);
            } catch (DotSecurityException | DotDataException e) {
                Logger.warn(DotFolderTransformerBuilder.class, String.format("Failed to lookup Folder with id `%s`",folderId), e);
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }


}
