package com.dotmarketing.portlets.fileassets.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

import java.util.List;

public interface FileAssetFactory {
    List<Contentlet>  findFileAssetsByFolderInDB(Folder parentFolder, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
}
