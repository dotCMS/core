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
     * takes a FileAssetSearcher object as params

     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */

    
    List<Contentlet> findByDB(FileAssetSearcher fileSearcher)
                    throws DotDataException, DotSecurityException;
}
