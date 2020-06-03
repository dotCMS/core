package com.dotcms.storage;

import com.dotcms.config.DotInitializer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;

import java.io.File;

/**
 * Init the {@link StorageProvider}
 * @author jsanca
 */
public class StorageProviderInitializer implements DotInitializer {

    @Override
    public void init() {

        this.initFileSystemStorage();
        this.initDbStorage();
    }

    private void initDbStorage() {

        final DataBaseStorage dataBaseStorage = new DataBaseStorage();
        APILocator.getFileStorageAPI().getStorageProvider().addStorage(StorageType.DB, dataBaseStorage);
    }

    private void initFileSystemStorage() {

        final FileSystemStorage fileSystemStorage = new FileSystemStorage();
        final String metadataBucketName = Config.getStringProperty("METADATA_BUCKET_NAME", "dotmetadata"); // todo: wrong one, change to METADATA_GROUP_NAME
        fileSystemStorage.addGroupMapping(metadataBucketName, new File(APILocator.getFileAssetAPI().getRealAssetsRootPath()));
        APILocator.getFileStorageAPI().getStorageProvider().addStorage(StorageType.FILE_SYSTEM, fileSystemStorage);
    }
}
