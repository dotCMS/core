package com.dotmarketing.portlets.fileassets.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

import java.util.List;

/**
 * This class provides methods to access information about directly from data base
 */
public interface FileAssetFactory {

    /**
     * Return the FileAsset into a {@link Folder}, it just return the FileAsset for which the user has permission.
     *
     * @param parentFolder Folder to search the files
     * @param user User to check permission
     * @param respectFrontendRoles it is true the Frontend roles are respect
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<Contentlet>  findFileAssetsByFolderInDB(Folder parentFolder, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
}
