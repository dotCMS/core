package com.dotcms.csspreproc;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import java.io.File;

class CompilerUtils {

    static Contentlet newFile(File file, Folder f, Host host) throws Exception {

        Contentlet fileAsset = new Contentlet();
        fileAsset.setStructureInode(CacheLocator.getContentTypeCache()
                .getStructureByVelocityVarName(
                        FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME).getInode());
        fileAsset.setHost(host.getIdentifier());
        fileAsset.setFolder(f.getInode());
        fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, file);
        fileAsset.setStringProperty(FileAssetAPI.TITLE_FIELD, file.getName());
        fileAsset.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, file.getName());
        fileAsset.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
        fileAsset = APILocator.getContentletAPI()
                .checkin(fileAsset, APILocator.getUserAPI().getSystemUser(), false);

        APILocator.getContentletAPI()
                .publish(fileAsset, APILocator.getUserAPI().getSystemUser(), false);
        APILocator.getContentletAPI().isInodeIndexed(fileAsset.getInode());
        APILocator.getContentletAPI().isInodeIndexed(fileAsset.getInode(), true);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            //Do nothing...
        }

        return fileAsset;
    }

}