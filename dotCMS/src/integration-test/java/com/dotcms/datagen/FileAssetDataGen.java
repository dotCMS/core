package com.dotcms.datagen;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import java.io.File;

public class FileAssetDataGen extends ContentletDataGen {

    public FileAssetDataGen(final Host host, final File file) throws DotSecurityException, DotDataException {
        this(file);
        this.host = host;
    }

    public FileAssetDataGen(final Folder folder, final File file)
            throws DotSecurityException, DotDataException {
        this(file);
        this.folder = folder;
    }

    private FileAssetDataGen(final File file) throws DotDataException, DotSecurityException {

        super(APILocator.getContentTypeAPI(APILocator.systemUser())
                .find("FileAsset").id());
        this.user = APILocator.systemUser();
        final String fileName = file.getName();
        setProperty(FileAssetAPI.TITLE_FIELD, fileName);
        setProperty(FileAssetAPI.FILE_NAME_FIELD, fileName);
        setProperty(FileAssetAPI.BINARY_FIELD, file);
    }

}