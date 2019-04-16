package com.dotcms.datagen;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import java.io.File;

public class FileAssetDataGen extends ContentletDataGen {

    public FileAssetDataGen(Host host, File file) throws DotSecurityException, DotDataException {
        this(file);
        this.host = host;
    }

    public FileAssetDataGen(Folder folder, File file)
            throws DotSecurityException, DotDataException {
        this(file);
        this.folder = folder;
    }

    private FileAssetDataGen(File file) throws DotDataException, DotSecurityException {

        super(APILocator.getContentTypeAPI(user)
                .find("FileAsset").id());

        setProperty(FileAssetAPI.TITLE_FIELD, file.getName());
        setProperty(FileAssetAPI.FILE_NAME_FIELD, file.getName());
        setProperty(FileAssetAPI.BINARY_FIELD, file);
    }

}