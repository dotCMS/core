package com.dotcms.api.id;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import io.vavr.control.Try;

public class ContentletIdSupplier implements IdSupplier {

    @Override
    public String getId(Object... incoming) {

        Contentlet contentlet = Try.of(()-> (Contentlet) incoming[0]).getOrElseThrow(e->new DotRuntimeException("Unable to create Contentlet id based on " + incoming, e));
        
        if(contentlet.isFileAsset()){
            return new FileAssetIdSupplier().getId(contentlet);
        }
        
        if(contentlet.isHTMLPage()){
            return new PageAssetIdSupplier().getId(contentlet);
        }
        
        if(contentlet.isHost()){
            return new HostIdSupplier().getId(contentlet);
        }
        
        return hasher("fileAsset", toHost(contentlet.getHost()).getHostname() , toFolder(contentlet.getFolder()).getPath() , contentlet.getStringProperty(HTMLPageAssetAPI.URL_FIELD));

    }


}
