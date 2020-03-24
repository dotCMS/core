package com.dotmarketing.portlets.fileassets.business;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileAssetFactoryImpl implements FileAssetFactory {
    private final PermissionAPI permissionAPI;

    private final String FIND_FILE_ASSETS_BY_FOLDER_QUERY =
            "SELECT contentlet.inode " +
            "FROM identifier, contentlet_version_info, contentlet, structure " +
            "WHERE identifier.parent_path = ? and " +
                "identifier.host_inode = ? and " +
                "contentlet_version_info.identifier = identifier.id and " +
                "contentlet_version_info.%s_inode = contentlet.inode and " +
                "contentlet.structure_inode = structure.inode and " +
                "structure.structuretype =4;";

    public FileAssetFactoryImpl() {
        permissionAPI = APILocator.getPermissionAPI();
    }

    @Override
    public List<Contentlet> findFileAssetsByFolderInDB(
            final Folder parentFolder,
            final  User user,
            final boolean live) throws DotDataException, DotSecurityException {

        final boolean respectFrontendRoles = live;
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL(String.format(FIND_FILE_ASSETS_BY_FOLDER_QUERY, live ? "live" : "working"));
        dotConnect.addParam(parentFolder.getPath());
        dotConnect.addParam(parentFolder.getHostId());

        if(!permissionAPI.doesUserHavePermission(parentFolder, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){
            throw new DotSecurityException("User:" + user.getUserId() + " does not have permissions on Folder " + parentFolder);
        }

        final List<Map<String, Object>> queryResults = dotConnect.loadObjectResults();
        return convertToFileAssets(user, respectFrontendRoles, queryResults);
    }

    @NotNull
    private List<Contentlet> convertToFileAssets(
            final User user,
            final boolean respectFrontendRoles,
            final List<Map<String, Object>> queryResults) throws DotDataException {

        final List<Contentlet> files = new ArrayList<>();

        for (final Map<String, Object> fileInfo : queryResults) {
            final String contentId = (String) fileInfo.get("inode");

            try {
                final Contentlet file = APILocator.getContentletAPI().find(contentId, user, respectFrontendRoles);
                files.add(file);
            } catch (DotSecurityException e) {
                Logger.debug(FileAssetFactoryImpl.class, e.getMessage());
            }
        }
        return files;
    }
}
