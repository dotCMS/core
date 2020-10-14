package com.dotcms.api.id;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import io.vavr.control.Try;

public class FileAssetIdSupplier implements IdSupplier {

    @Override
    public String getId(Object... incoming) {
        Contentlet contentlet = Try.of(()-> (Contentlet) incoming[0]).getOrElseThrow(e->new DotRuntimeException("Unable to create FileAsset id based on " + incoming, e));

        if (!contentlet.isFileAsset()) {
            throw new DotRuntimeException("Not a File Asset to create id based on " + incoming);
        }
        
        return hasher("fileAsset", toHost(contentlet.getHost()).getHostname() , toFolder(contentlet.getFolder()).getPath() , contentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD));
        

    }


}
