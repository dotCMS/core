package com.dotmarketing.portlets.contentlet.transform;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.util.UtilMethods.isSet;

import com.dotmarketing.beans.IconType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionAPI.Type;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.ejb.UserLocalManagerUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
        GRAPHQL_VIEW,
        CONTENT_DRIVE
    }
    private final User user;
    private final Role[] roles;
    private final List<Folder> folders;
    private final TargetView targetView;

    /**
     * @param user
     * @param folders
     */
    DotFolderTransformerImpl(
            final User user,
            final Role[] roles,
            final List<Folder> folders,
            final TargetView targetView) {
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
        this(user, roles, folders , TargetView.BROWSE_VIEW);
    }

    /**
     * this builds a view for folderToMapTransformerView
     * @param folders
     */
    public DotFolderTransformerImpl(final List<Folder> folders) {
        this(null, null, folders, TargetView.GRAPHQL_VIEW);
    }

    /**
     * Returns a default instance of the {@code DotMapViewTransformer} configured with the provided user, roles,
     * and folders for processing views, specifically targeted at the Content Drive API.
     * This factory method simplifies creation and initialization of the transformer object.
     *
     * @param user the user to be associated with the transformer
     * @param roles an array of roles related to the user
     * @param folders a list of folders to be processed by the transformer
     * @return an instance of {@code DotMapViewTransformer} configured for the provided inputs
     */
    public static DotMapViewTransformer defaultInstance(final User user, final Role[] roles, final List<Folder> folders) {
        return new DotFolderTransformerImpl(user, roles, folders, TargetView.CONTENT_DRIVE);
    }

    @VisibleForTesting
    DotFolderTransformerImpl(final List<Folder> folders, final TargetView targetView) {
        this(null, null, folders, targetView);
    }

    List<Map<String,Object>> transform() {
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
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
                        Logger.error(DotFolderTransformerImpl.class,String.format("Error building Map view for GraphQL of folder with id `%s`", folder.getIdentifier()),e);
                    }
                }
             break;
            }
            default: {
                for(final Folder folder:folders) {
                    try {
                        final List<Integer> permissions = permissionAPI.getPermissionIdsFromRoles(folder, roles, user);
                        if(permissions.contains(PERMISSION_READ)){
                            maps.add(contentDriveView(folder, permissions));
                        }
                    } catch (Exception e){
                        Logger.error(DotFolderTransformerImpl.class,String.format("Error building Map view for ContentDRive of folder with id `%s`", folder.getIdentifier()),e);
                    }
                }
            }

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
        map.put("parent", folder.getIdentifier());
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
     * This method builds the view expected by Content Drive API
     * @param folder folder domain object
     * @param permissions set of user permissions
     * @return MapView of the folder
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Map<String, Object> contentDriveView(final Folder folder, final List<Integer> permissions)
            throws DotSecurityException, DotDataException {
        final List<String> stringPermissions = permissions.stream()
                .map(integer -> Try.of(() -> Type.findById(integer).name())
                        .getOrNull()).filter(Objects::nonNull).collect(Collectors.toList());
        final Map<String, Object> map = new HashMap<>(folder.getMap());
        map.put("permissions", stringPermissions);
        map.remove("inode");
        final String ownerId = folder.getOwner();
        if (null != ownerId) {
            if ("system".equalsIgnoreCase(ownerId)) {
                map.put("owner", "System");
            } else {
                final User owner = Try.of(() -> UserLocalManagerUtil.getUserById(ownerId))
                        .getOrNull();
                if (null != owner) {
                    map.put("owner", owner.getFullName());
                } else {
                    map.put("owner", "unknown");
                }
            }
        }
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
