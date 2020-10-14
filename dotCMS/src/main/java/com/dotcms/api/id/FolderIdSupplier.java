package com.dotcms.api.id;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.folders.model.Folder;
import io.vavr.control.Try;

public class FolderIdSupplier implements IdSupplier {


    @Override
    public String getId(Object... incoming) {
        Folder folder = Try.of(()-> (Folder) incoming[0]).getOrElseThrow(e->new DotRuntimeException("Unable to create Folder id based on " + incoming, e));

        
        
        
        return hasher("folder", toHost(folder.getHostId()).getHostname(), toFolder(folder.getParentFolderId()).getPath(), folder.getName() );
        

    }
    
    
    


}
