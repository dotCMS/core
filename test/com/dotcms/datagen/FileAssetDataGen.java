package com.dotcms.datagen;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;

import java.io.File;

public class FileAssetDataGen extends ContentletDataGen {

    public FileAssetDataGen(Host host, File file) {
        this(file);
        this.host = host;
    }

    public FileAssetDataGen(Folder folder, File file) {
        this(file);
        this.folder = folder;
    }

    private FileAssetDataGen(File file) {
        super(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_INODE);
        setProperty(FileAssetAPI.TITLE_FIELD, file.getName());
        setProperty(FileAssetAPI.FILE_NAME_FIELD, file.getName());
        setProperty(FileAssetAPI.BINARY_FIELD, file);
    }
}
