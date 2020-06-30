package com.dotmarketing.portlets.contentlet.transform;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.util.UtilMethods.isSet;

import com.dotmarketing.beans.IconType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Folder Transformers Class
 */
public class DotFolderTransformerImpl implements DotMapViewTransformer {

    @Override
    public List<Map<String, Object>> toMaps() {
        return transform();
    }

    private enum TargetView {
        BROWSE_VIEW,
        GRAPHQL_VIEW
    }
    private PermissionAPI permissionAPI;

    private final User user;
    private final Role[] roles;
    private final List<Folder> folders;
    private final TargetView targetView;

    /**
     * Test friendly "private" constructor
     * @param permissionAPI
     * @param user
     * @param folders
     */
    @VisibleForTesting
    DotFolderTransformerImpl(
            final PermissionAPI permissionAPI,
            final User user,
            final Role[] roles,
            final List<Folder> folders,
            final TargetView targetView) {
        this.permissionAPI = permissionAPI;
        this.user = user;
        this.roles =   isSet(roles) ? Arrays.copyOf(roles, roles.length) : new Role[]{} ;
        this.folders = folders;
        this.targetView = targetView;
    }

    /**
     * This builds the view for siteBrowser
     * @param user
     * @param folders
     */
    public DotFolderTransformerImpl(final User user, final Role[] roles, final List<Folder> folders) {
        this(APILocator.getPermissionAPI(), user, roles, folders , TargetView.BROWSE_VIEW);
    }

    /**
     * this builds a view for folderToMapTransformerView
     * @param folders
     */
    public DotFolderTransformerImpl(final List<Folder> folders) {
        this(APILocator.getPermissionAPI(), null, null, folders, TargetView.GRAPHQL_VIEW);
    }

    List<Map<String,Object>> transform() {
        final List<Map<String,Object>> maps = new ArrayList<>();
        switch (targetView){
            case BROWSE_VIEW:{
              for(final Folder folder:folders) {
                 try {
                     final List<Integer> permissions = permissionAPI.getPermissionIdsFromRoles(folder, roles, user);
                     if(permissions.contains(PERMISSION_READ)){
                        maps.add(buildSiteBrowserView(folder, permissions));
                     }
                 } catch (Exception e){
                     Logger.error(DotFolderTransformerImpl.class,String.format("Error building Map view of folder with id `%s`", folder.getIdentifier()),e);
                 }
              }
              break;
            }
            case GRAPHQL_VIEW:{
                for(final Folder folder:folders) {
                    try {
                        maps.add(buildFolderToMapTransformerView(folder));
                    } catch (Exception e){
                        Logger.error(DotFolderTransformerImpl.class,String.format("Error building Map view of folder with id `%s`", folder.getIdentifier()),e);
                    }
                }
             break;
            }
            default:
                throw new IllegalStateException("Unexpected value: " + targetView);
        }
        return maps;
    }

    /**
     * This method builds the view expected by Browse API
     * @param folder
     * @param permissions
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Map<String, Object> buildSiteBrowserView(final Folder folder, final List<Integer> permissions)
            throws DotSecurityException, DotDataException {

        final Map<String, Object> map = new HashMap<>(folder.getMap());
        map.put("permissions", permissions);
        map.put("parent", folder.getInode());
        map.put("mimeType", StringPool.BLANK);
        map.put("name", folder.getName());
        map.put("title", folder.getName());
        map.put("description", folder.getTitle());
        map.put("extension", "folder");
        map.put("hasTitleImage", StringPool.BLANK);
        map.put("__icon__", IconType.FOLDER.iconName());

        return map;

    }

    /**
     * This method builds the view expected by the GraphQL Data fetcher
     * @param folder
     * @return
     */
    private Map<String, Object> buildFolderToMapTransformerView(final Folder folder) {

        final Map<String, Object> map = new HashMap<>();

        map.put("folderId", folder.getInode());
        map.put("folderFileMask", folder.getFilesMasks());
        map.put("folderSortOrder", folder.getSortOrder());
        map.put("folderName", folder.getName());
        map.put("folderPath", folder.getPath());
        map.put("folderTitle", folder.getTitle());
        map.put("folderDefaultFileType", folder.getDefaultFileType());

        return ImmutableMap.of("folder", folder, "folderMap", map);
    }

}
