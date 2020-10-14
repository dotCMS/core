package com.dotcms.api.id;


import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UUIDGenerator;
import io.vavr.control.Try;

public class IdentityProvider {


    final public String idHash;

    public IdentityProvider(Object objectToId, Folder parent) {
        this.idHash = id(objectToId, parent);
    }
    
    public IdentityProvider(Object objectToId, Host hostParent) {
        Folder parent = Try.of(()->APILocator.getFolderAPI().findSystemFolder()).getOrElseThrow(DotRuntimeException::new);
        
        this.idHash = objectToId instanceof Folder 
                        ? idFolder((Folder) objectToId, parent)
                        : objectToId instanceof Contentlet 
                            ? idContentlet((Contentlet) objectToId, parent)
                            : id(objectToId, parent);


    }
    
    /**
     * 
     * @param objectToId
     * @param parent
     * @return
     */
    String idContentlet(Contentlet objectToId, Folder parent) {

        return new ContentletIdSupplier().getId(objectToId);

    }

    String idFolder(Folder objectToId, Folder parent) {
        objectToId.setParentFolderId(parent.getInode());
        return new FolderIdSupplier().getId(objectToId);

    }

    String id(Object objectToId, Folder parent) {

        return UUIDGenerator.generateUuid();

    }


}
