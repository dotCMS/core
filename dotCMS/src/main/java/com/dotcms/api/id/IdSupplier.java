package com.dotcms.api.id;

import org.apache.commons.codec.digest.DigestUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import io.vavr.control.Try;

public interface IdSupplier {


    String getId(Object... incoming);

    default Folder toFolder(final String folderId) {
        return Try.of(()->APILocator.getFolderAPI().find(folderId, APILocator.systemUser(), false)).getOrElseThrow(DotRuntimeException::new);
    }

    default Host toHost(final String hostId) {
        return Try.of(()->APILocator.getHostAPI().find(hostId, APILocator.systemUser(), false)).getOrElseThrow(DotRuntimeException::new);
    }

    
    default String hasher(final String...strings ) {
        final String toHash = String.join(":", strings).toLowerCase();
        final String hash =  DigestUtils.sha256Hex(toHash);
        Logger.info(IdSupplier.class, "hashing identifier for:'"  + toHash + "' --> '" + hash + "'");
        return UUIDUtil.uuidIfy(hash.substring(0, Math.min(32, hash.length())));
        
        
    }
    
    
}
